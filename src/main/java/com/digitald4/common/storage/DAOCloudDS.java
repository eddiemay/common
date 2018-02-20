package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.RetryableFunction;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class DAOCloudDS implements DAO {

	private final Datastore datastore;
	private final Map<Class<?>, KeyFactory> keyFactories = new HashMap<>();
	private static final Map<Class<?>, Message> defaultInstances = new HashMap<>();

	public DAOCloudDS() {
		this.datastore = DatastoreOptions.getDefaultInstance().getService();
	}

	@Override
	public <T extends Message> T create(T t) {
		return convert(t.getClass(), new RetryableFunction<T, Entity>() {
			@Override
			public Entity apply(T t) {
				Object id = t.getField(t.getDescriptorForType().findFieldByName("id"));
				Entity.Builder entity;
				if (id instanceof Long && (Long) id != 0L) {
					entity = Entity.newBuilder(getKeyFactory(t.getClass()).newKey((Long) id));
				} else if (id instanceof String && !((String) id).isEmpty()) {
					entity = Entity.newBuilder(getKeyFactory(t.getClass()).newKey((String) id));
			  } else {
					entity = Entity.newBuilder(datastore.allocateId(getKeyFactory(t.getClass()).newKey()));
				}
				t.getAllFields()
						.forEach((field, value) -> setObject(entity, t, field, value));
				 return datastore.put(entity.build());
			}
		}.applyWithRetries(t));
	}

	@Override
	public <T extends Message> T get(Class<T> c, long id) {
		return new RetryableFunction<Long, T>() {
			@Override
			public T apply(Long id) {
				return convert(c, datastore.get(getKeyFactory(c).newKey(id)));
			}
		}.applyWithRetries(id);
	}

	@Override
	public <T extends Message> QueryResult<T> list(Class<T> c, DD4Protos.Query query) {
		return new RetryableFunction<DD4Protos.Query, QueryResult<T>>() {
			@Override
			public QueryResult<T> apply(DD4Protos.Query request) {
				EntityQuery.Builder query = Query.newEntityQueryBuilder()
						.setKind(c.getSimpleName());
				if (request.getFilterCount() > 0) {
					Descriptor descriptor = getDefaultInstance(c).getDescriptorForType();
					if (request.getFilterCount() == 1) {
						Filter filter = request.getFilter(0);
						query.setFilter(convertToPropertyFilter(filter, descriptor));
					} else {
						List<PropertyFilter> pfilters = request.getFilterList()
								.stream()
								.map(filter -> convertToPropertyFilter(filter, descriptor))
								.collect(Collectors.toList());
						query.setFilter(CompositeFilter.and(pfilters.get(0),
								pfilters.subList(1, pfilters.size()).toArray(new PropertyFilter[pfilters.size() - 1])));
					}
				}
				/* Rather than use limit, loop over all items and add until limit to get a total count.
				if (request.getLimit() > 0) {
					query.setLimit(request.getLimit());
				}*/
				request.getOrderByList().forEach(orderBy -> query.addOrderBy(orderBy.getDesc()
						? OrderBy.desc(orderBy.getColumn()) : OrderBy.asc(orderBy.getColumn())));
				List<T> results = new ArrayList<>();
				int[] count = new int[1];
				datastore.run(query.build()).forEachRemaining(entity -> {
					if (count[0] >= request.getOffset()
							&& (request.getLimit() == 0 || count[0] < request.getOffset() + request.getLimit())) {
						results.add(convert(c, entity));
					}
					count[0]++;
				});
				return new QueryResult<>(results, request.getOffset() + count[0]);
			}
		}.applyWithRetries(query);
	}

	@Override
	public <T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return new RetryableFunction<Pair<Long, UnaryOperator<T>>, T>() {
			@Override
			public T apply(Pair<Long, UnaryOperator<T>> pair) {
				T t = updater.apply(get(c, id));
				Entity.Builder entity = Entity.newBuilder(getKeyFactory(c).newKey(id));
				t.getAllFields()
						.forEach((field, value) -> setObject(entity, t, field, value));
				return convert(c, datastore.put(entity.build()));
			}
		}.applyWithRetries(Pair.of(id, updater));
	}

	@Override
	public <T> void delete(Class<T> c, long id) {
		deleteFunction.applyWithRetries(Pair.of(c, id));
	}

	@Override
	public <T extends Message> int delete(Class<T> c, DD4Protos.Query query) {
		return new RetryableFunction<DD4Protos.Query, Integer>() {
			@Override
			public Integer apply(DD4Protos.Query request) {
				QueryResult<T> results = list(c, request);
				if (results.size() > 0) {
					FieldDescriptor idField = DAOCloudDS.getDefaultInstance(c)
							.getDescriptorForType().findFieldByName("id");
					results.parallelStream().forEach(t -> delete(c, (Long) t.getField(idField)));
				}
				return results.size();
			}
		}.applyWithRetries(query);
	}

	public static <T extends Message> T getDefaultInstance(Class<?> c) {
		T defaultInstance = (T) defaultInstances.get(c);
		if (defaultInstance == null) {
			try {
				defaultInstance = (T) c.getMethod("getDefaultInstance").invoke(null);
				defaultInstances.put(c, defaultInstance);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		return defaultInstance;
	}

	private KeyFactory getKeyFactory(Class<?> c) {
		return keyFactories.computeIfAbsent(c, v -> datastore.newKeyFactory().setKind(c.getSimpleName()));
	}

	private RetryableFunction<Pair<Class<?>, Long>, Boolean> deleteFunction = new RetryableFunction<Pair<Class<?>, Long>, Boolean>() {
		@Override
		public Boolean apply(Pair<Class<?>, Long> pair) {
			datastore.delete(getKeyFactory(pair.getLeft()).newKey(pair.getRight()));
			return true;
		}
	};

	private static <T extends Message> void setObject(Entity.Builder entity, T t, FieldDescriptor field,
																															 Object value) {
		String name = field.getName();
		if (name.equals("id")) {
			return;
		}
		if (field.isRepeated() || field.isMapField()) {
			JSONObject json = new JSONObject(JsonFormat.printer().print(t));
			entity.set(name, json.get(FormatText.toLowerCamel(field.getName())).toString());
		} else {
			switch (field.getJavaType()) {
				case BOOLEAN: entity.set(name, (Boolean) value); break;
				case DOUBLE: entity.set(name, (Double) value); break;
				case ENUM: entity.set(name, value.toString()); break;
				case FLOAT: entity.set(name, (Float) value); break;
				case INT: entity.set(name, (Integer) value); break;
				case LONG:
					if (name.endsWith("id")) {
						entity.set(name, (Long) value);
					} else {
						entity.set(name, Timestamp.of(new java.sql.Timestamp((Long) value)));
					}
					break;
				case MESSAGE:
					entity.set(name, JsonFormat.printer().print((Message) value));
					break;
				case STRING: entity.set(name, (String) value); break;
				case BYTE_STRING:
				default: entity.set(name, value.toString());
			}
		}
	}

	private <T extends Message> T convert(Class<?> c, Entity entity) {
		if (entity == null) {
			return null;
		}
		Message.Builder builder = getDefaultInstance(c).toBuilder();
		Descriptor descriptor = builder.getDescriptorForType();
		FieldDescriptor idField = descriptor.findFieldByName("id");
		if (idField != null) {
			builder.setField(idField, entity.getKey().getId());
		}
		for (String columnName : entity.getNames()) {
			try {
				FieldDescriptor field = descriptor.findFieldByName(columnName);
				if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
					JsonFormat.parser().ignoringUnknownFields()
							.merge("{\"" + field.getName() + "\": " + entity.getString(columnName) + "}", builder);
				} else {
					switch (field.getJavaType()) {
						case BOOLEAN: builder.setField(field, entity.getBoolean(columnName)); break;
						case BYTE_STRING:
							builder.setField(field, ByteString.copyFrom(entity.getBlob(columnName).toByteArray())); break;
						case DOUBLE: builder.setField(field, entity.getDouble(columnName)); break;
						case ENUM:
							builder.setField(field, field.getEnumType().findValueByName(entity.getString(columnName))); break;
						case FLOAT: builder.setField(field, (float) entity.getDouble(columnName)); break;
						case INT: builder.setField(field, (int) entity.getLong(columnName)); break;
						case LONG:
							if (columnName.endsWith("id")) {
								builder.setField(field, entity.getLong(columnName));
							} else {
								builder.setField(field, entity.getTimestamp(columnName).toSqlTimestamp().getTime());
							}
							break;
						case STRING: builder.setField(field, entity.getString(columnName)); break;
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage() + " for column: " + columnName + ". value: " + entity.getValue(columnName));
			}
		}
		return (T) builder.build();
	}

	private PropertyFilter convertToPropertyFilter(Filter filter, Descriptor descriptor) {
		String columName = filter.getColumn();
		FieldDescriptor field = descriptor.findFieldByName(columName);
		if (field == null) {
			throw new DD4StorageException("Unknown column: " + columName);
		}
		switch (filter.getOperator()) {
			case "<" : {
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.lt(columName, Boolean.valueOf(filter.getValue()));
					case DOUBLE: return PropertyFilter.lt(columName, Double.valueOf(filter.getValue()));
					case INT: return PropertyFilter.lt(columName, Integer.valueOf(filter.getValue()));
					case LONG: return PropertyFilter.lt(columName, Long.valueOf(filter.getValue()));
					case STRING: return PropertyFilter.lt(columName, filter.getValue());
					default: return PropertyFilter.lt(columName, filter.getValue());
				}
			}
			case "<=" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.le(columName, Boolean.valueOf(filter.getValue()));
					case DOUBLE: return PropertyFilter.le(columName, Double.valueOf(filter.getValue()));
					case INT: return PropertyFilter.le(columName, Integer.valueOf(filter.getValue()));
					case LONG: return PropertyFilter.le(columName, Long.valueOf(filter.getValue()));
					case STRING: return PropertyFilter.le(columName, filter.getValue());
					default: return PropertyFilter.le(columName, filter.getValue());
				}
			case "=" :
			case "" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.eq(columName, Boolean.valueOf(filter.getValue()));
					case DOUBLE: return PropertyFilter.eq(columName, Double.valueOf(filter.getValue()));
					case INT: return PropertyFilter.eq(columName, Integer.valueOf(filter.getValue()));
					case LONG: return PropertyFilter.eq(columName, Long.valueOf(filter.getValue()));
					case STRING: return PropertyFilter.eq(columName, filter.getValue());
					default: return PropertyFilter.eq(columName, filter.getValue());
				}
			case ">=" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.ge(columName, Boolean.valueOf(filter.getValue()));
					case DOUBLE: return PropertyFilter.ge(columName, Double.valueOf(filter.getValue()));
					case INT: return PropertyFilter.ge(columName, Integer.valueOf(filter.getValue()));
					case LONG: return PropertyFilter.ge(columName, Long.valueOf(filter.getValue()));
					case STRING: return PropertyFilter.ge(columName, filter.getValue());
					default: PropertyFilter.ge(columName, filter.getValue());
				}
			case ">" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.gt(columName, Boolean.valueOf(filter.getValue()));
					case DOUBLE: return PropertyFilter.gt(columName, Double.valueOf(filter.getValue()));
					case INT: return PropertyFilter.gt(columName, Integer.valueOf(filter.getValue()));
					case LONG: return PropertyFilter.gt(columName, Long.valueOf(filter.getValue()));
					case STRING: return PropertyFilter.gt(columName, filter.getValue());
					default: return PropertyFilter.gt(columName, filter.getValue());
				}
			default: throw new IllegalArgumentException("Unknown operator " + filter.getOperator());
		}
	}
}
