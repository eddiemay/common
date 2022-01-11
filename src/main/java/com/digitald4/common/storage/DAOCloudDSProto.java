package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.ProtoUtil;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import org.json.JSONObject;

public class DAOCloudDSProto implements TypedDAO<Message> {
	private static final Map<Class<?>, KeyFactory> keyFactories = new HashMap<>();
	private final Datastore datastore;

	@Inject
	public DAOCloudDSProto(Datastore datastore) {
		this.datastore = datastore;
	}

	@Override
	public <T extends Message> T create(T t) {
		return Calculate.executeWithRetries(2, () -> {
			Object id = t.getField(t.getDescriptorForType().findFieldByName("id"));
			Entity.Builder entity;
			if (id instanceof Long && (Long) id != 0L) {
				entity = Entity.newBuilder(getKeyFactory(t.getClass()).newKey((Long) id));
			} else if (id instanceof String && !((String) id).isEmpty()) {
				entity = Entity.newBuilder(getKeyFactory(t.getClass()).newKey((String) id));
			} else {
				entity = Entity.newBuilder(datastore.allocateId(getKeyFactory(t.getClass()).newKey()));
			}
			t.getAllFields().forEach((field, value) -> setObject(entity, t, field, value));

			return (T) convert(t.getClass(), datastore.put(entity.build()));
		});
	}

	@Override
	public <T extends Message> ImmutableList<T> create(Iterable<T> entities) {
		return stream(entities).map(this::create).collect(toImmutableList());
	}

	@Override
	public <T extends Message> T get(Class<T> c, long id) {
		return Calculate.executeWithRetries(2, () -> convert(c, datastore.get(getKeyFactory(c).newKey(id))));
	}

	@Override
	public <T extends Message> ImmutableList<T> get(Class<T> c, Iterable<Long> ids) {
		KeyFactory keyFactory = getKeyFactory(c);
		return Calculate.executeWithRetries(
				2, () -> convert(c, datastore.get(stream(ids).map(keyFactory::newKey).collect(toImmutableList()))));
	}

	@Override
	public <T extends Message> QueryResult<T> list(Class<T> c, Query.List query) {
		return Calculate.executeWithRetries(2, () -> {
			EntityQuery.Builder eQuery = com.google.cloud.datastore.Query.newEntityQueryBuilder().setKind(c.getSimpleName());
			if (!query.getFilters().isEmpty()) {
				Descriptor descriptor = ProtoUtil.getDefaultInstance(c).getDescriptorForType();
				if (query.getFilters().size() == 1) {
					Filter filter = query.getFilters().get(0);
					eQuery.setFilter(convertToPropertyFilter(filter, descriptor));
				} else {
					ImmutableList<PropertyFilter> pfilters = query.getFilters()
							.stream()
							.map(filter -> convertToPropertyFilter(filter, descriptor))
							.collect(toImmutableList());
					eQuery.setFilter(CompositeFilter.and(pfilters.get(0),
							pfilters.subList(1, pfilters.size()).toArray(new PropertyFilter[pfilters.size() - 1])));
				}
			}
			/* Rather than use limit, loop over all items and add until limit to get a total count.
			if (request.getLimit() > 0) {
				query.setLimit(request.getLimit());
			}*/
			query.getOrderBys().forEach(orderBy ->
					eQuery.addOrderBy(orderBy.getDesc() ? OrderBy.desc(orderBy.getColumn()) : OrderBy.asc(orderBy.getColumn())));
			ImmutableList.Builder<T> results = ImmutableList.builder();
			AtomicInteger count = new AtomicInteger();
			int end = query.getLimit() == 0 ? Integer.MAX_VALUE : query.getOffset() + query.getLimit();
			datastore.run(eQuery.build()).forEachRemaining(entity -> {
				if (count.getAndIncrement() >= query.getOffset() && count.get() <= end) {
					results.add(convert(c, entity));
				}
			});

			return QueryResult.of(results.build(), query.getOffset() + count.get(), query);
		});
	}

	@Override
	public <T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return Calculate.executeWithRetries(2, () -> {
			T t = updater.apply(get(c, id));
			Entity.Builder entity = Entity.newBuilder(getKeyFactory(c).newKey(id));
			t.getAllFields().forEach((field, value) -> setObject(entity, t, field, value));
			return convert(c, datastore.put(entity.build()));
		});
	}

	@Override
	public <T extends Message> ImmutableList<T> update(Class<T> c, Iterable<Long> ids, UnaryOperator<T> updater) {
		KeyFactory keyFactory = getKeyFactory(c);
		return Calculate.executeWithRetries(2,
				() -> get(c, ids).stream()
						.map(updater)
						.map(t -> {
							long id = (Long) t.getField(t.getDescriptorForType().findFieldByName("id"));
							Entity.Builder entity = Entity.newBuilder(keyFactory.newKey(id));
							t.getAllFields().forEach((field, value) -> setObject(entity, t, field, value));
							return convert(c, datastore.put(entity.build()));
						})
						.collect(toImmutableList()));
	}

	@Override
	public <T extends Message> void delete(Class<T> c, long id) {
		Calculate.executeWithRetries(2, () -> {
			datastore.delete(getKeyFactory(c).newKey(id));
			return true;
		});
	}

	@Override
	public <T extends Message> void delete(Class<T> c, Iterable<Long> ids) {
		Calculate.executeWithRetries(2, () -> {
			KeyFactory keyFactory = getKeyFactory(c);
			stream(ids).map(keyFactory::newKey).forEach(datastore::delete);
			return true;
		});
	}

	private <T extends Message> void setObject(Entity.Builder entity, T t, FieldDescriptor field, Object value) {
		String name = field.getName();
		if (name.equals("id")) {
			return;
		}

		if (field.isRepeated() || field.isMapField()) {
			JSONObject json = new JSONObject(ProtoUtil.print(t));
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
					entity.set(name, ProtoUtil.print((Message) value));
					break;
				case STRING: entity.set(name, (String) value); break;
				case BYTE_STRING:
				default: entity.set(name, value.toString());
			}
		}
	}

	private <T extends Message> ImmutableList<T> convert(Class<T> c, Iterator<Entity> entities) {
		return stream(entities).map(entity -> convert(c, entity)).collect(toImmutableList());
	}

	private <T extends Message> T convert(Class<T> c, Entity entity) {
		if (entity == null) {
			return null;
		}

		Message.Builder builder = ProtoUtil.getDefaultInstance(c).toBuilder();
		Descriptor descriptor = builder.getDescriptorForType();
		FieldDescriptor idField = descriptor.findFieldByName("id");
		if (idField != null) {
			builder.setField(idField, entity.getKey().getId());
		}
		for (String colName : entity.getNames()) {
			try {
				FieldDescriptor field = descriptor.findFieldByName(colName);
				if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
					ProtoUtil.merge("{\"" + field.getName() + "\": " + entity.getString(colName) + "}", builder);
				} else {
					switch (field.getJavaType()) {
						case BOOLEAN: builder.setField(field, entity.getBoolean(colName)); break;
						case BYTE_STRING: builder.setField(field, ByteString.copyFrom(entity.getBlob(colName).toByteArray())); break;
						case DOUBLE: builder.setField(field, entity.getDouble(colName)); break;
						case ENUM: builder.setField(field, field.getEnumType().findValueByName(entity.getString(colName))); break;
						case FLOAT: builder.setField(field, (float) entity.getDouble(colName)); break;
						case INT: builder.setField(field, (int) entity.getLong(colName)); break;
						case LONG:
							if (colName.endsWith("id")) {
								builder.setField(field, entity.getLong(colName));
							} else {
								builder.setField(field, entity.getTimestamp(colName).toSqlTimestamp().getTime());
							}
							break;
						case STRING: builder.setField(field, entity.getString(colName)); break;
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage() + " for column: " + colName + ". value: " + entity.getValue(colName));
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
		String value = filter.getValue().toString();
		switch (filter.getOperator()) {
			case "<" : {
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.lt(columName, Boolean.parseBoolean(value));
					case FLOAT:
					case DOUBLE: return PropertyFilter.lt(columName, Double.parseDouble(value));
					case INT: return PropertyFilter.lt(columName, Integer.parseInt(value));
					case LONG: return PropertyFilter.lt(columName, Long.parseLong(value));
					case STRING: return PropertyFilter.lt(columName, value);
					default: return PropertyFilter.lt(columName, value);
				}
			}
			case "<=" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.le(columName, Boolean.parseBoolean(value));
					case FLOAT:
					case DOUBLE: return PropertyFilter.le(columName, Double.parseDouble(value));
					case INT: return PropertyFilter.le(columName, Integer.parseInt(value));
					case LONG: return PropertyFilter.le(columName, Long.parseLong(value));
					case STRING: return PropertyFilter.le(columName, value);
					default: return PropertyFilter.le(columName, value);
				}
			case "=" :
			case "" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.eq(columName, Boolean.parseBoolean(value));
					case FLOAT:
					case DOUBLE: return PropertyFilter.eq(columName, Double.parseDouble(value));
					case INT: return PropertyFilter.eq(columName, Integer.parseInt(value));
					case LONG: return PropertyFilter.eq(columName, Long.parseLong(value));
					case STRING: return PropertyFilter.eq(columName, value);
					default: return PropertyFilter.eq(columName, value);
				}
			case ">=" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.ge(columName, Boolean.parseBoolean(value));
					case FLOAT:
					case DOUBLE: return PropertyFilter.ge(columName, Double.parseDouble(value));
					case INT: return PropertyFilter.ge(columName, Integer.parseInt(value));
					case LONG: return PropertyFilter.ge(columName, Long.parseLong(value));
					case STRING: return PropertyFilter.ge(columName, value);
					default: PropertyFilter.ge(columName, value);
				}
			case ">" :
				switch (field.getJavaType()) {
					case BOOLEAN: return PropertyFilter.gt(columName, Boolean.parseBoolean(value));
					case FLOAT:
					case DOUBLE: return PropertyFilter.gt(columName, Double.parseDouble(value));
					case INT: return PropertyFilter.gt(columName, Integer.parseInt(value));
					case LONG: return PropertyFilter.gt(columName, Long.parseLong(value));
					case STRING: return PropertyFilter.gt(columName, value);
					default: return PropertyFilter.gt(columName, value);
				}
			default: throw new IllegalArgumentException("Unknown operator " + filter.getOperator());
		}
	}

	private KeyFactory getKeyFactory(Class<?> c) {
		return keyFactories.computeIfAbsent(c, v -> datastore.newKeyFactory().setKind(c.getSimpleName()));
	}
}
