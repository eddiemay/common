package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.RetryableFunction;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DataConnectorCloudDS implements DataConnector {

	private final Datastore datastore;
	private final Map<Class<?>, KeyFactory> keyFactories = new HashMap<>();
	private final Map<Class<?>, GeneratedMessageV3> defaultInstances = new HashMap<>();

	public DataConnectorCloudDS() {
		this.datastore = DatastoreOptions.getDefaultInstance().getService();
	}

	@Override
	public <T extends GeneratedMessageV3> T create(T t) {
		return new RetryableFunction<T, T>() {
			@Override
			public T apply(T t) {
				Entity.Builder entity = Entity.newBuilder(datastore.allocateId(getKeyFactory(t.getClass()).newKey()));
				t.getAllFields()
						.forEach((key, value) -> set(entity, key.getName(), value));
				return convert(t.getClass(), datastore.put(entity.build()));
			}
		}.applyWithRetries(t);
	}

	@Override
	public <T extends GeneratedMessageV3> T get(Class<T> c, long id) {
		return new RetryableFunction<Long, T>() {
			@Override
			public T apply(Long id) {
				return convert(c, datastore.get(getKeyFactory(c).newKey(id)));
			}
		}.applyWithRetries(id);
	}

	@Override
	public <T extends GeneratedMessageV3> ListResponse<T> list(Class<T> c, ListRequest listRequest) {
		return new RetryableFunction<ListRequest, ListResponse<T>>() {
			@Override
			public ListResponse<T> apply(ListRequest request) {
				EntityQuery.Builder query = Query.newEntityQueryBuilder()
						.setKind(c.getSimpleName())
						.setOffset(request.getPageToken());
				if (request.getPageSize() > 0) {
					query.setLimit(request.getPageSize());
				}
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
				request.getOrderByList().forEach(orderBy -> query.addOrderBy(orderBy.getDesc()
						? OrderBy.desc(orderBy.getColumn()) : OrderBy.asc(orderBy.getColumn())));
				ListResponse.Builder<T> listResponse = ListResponse.newBuilder();
				datastore.run(query.build()).forEachRemaining(entity -> listResponse.addResult(convert(c, entity)));
				return listResponse.build();
			}
		}.apply(listRequest);
	}

	private PropertyFilter convertToPropertyFilter(Filter filter, Descriptor descriptor) {
		String columName = filter.getColumn();
		FieldDescriptor field = descriptor.findFieldByName(columName);
		if (field == null) {
			throw new DD4StorageException("Unknown column: " + columName);
		}
		switch (field.getJavaType()) {
			case LONG: return PropertyFilter.eq(columName, Long.valueOf(filter.getValue()));
			case INT: return PropertyFilter.eq(columName, Integer.valueOf(filter.getValue()));
			case DOUBLE: return PropertyFilter.eq(columName, Double.valueOf(filter.getValue()));
			case BOOLEAN: return PropertyFilter.eq(columName, Boolean.valueOf(filter.getValue()));
		}
		return PropertyFilter.eq(columName, filter.getValue());
	}

	@Override
	public <T extends GeneratedMessageV3> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return new RetryableFunction<Pair<Long, UnaryOperator<T>>, T>() {
			@Override
			public T apply(Pair<Long, UnaryOperator<T>> pair) {
				T t = updater.apply(get(c, id));
				Entity.Builder entity = Entity.newBuilder(getKeyFactory(c).newKey(id));
				t.getAllFields()
						.forEach((key, value) -> set(entity, key.getName(), value));
				return convert(c, datastore.put(entity.build()));
			}
		}.apply(Pair.of(id, updater));
	}

	@Override
	public <T> void delete(Class<T> c, long id) {
		new RetryableFunction<Long, Boolean>() {
			@Override
			public Boolean apply(Long id) {
				datastore.delete(getKeyFactory(c).newKey(id));
				return true;
			}
		};
	}

	public <T extends GeneratedMessageV3> T getDefaultInstance(Class<?> c) {
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

	private static void set(Entity.Builder entity, String name, Object value) {
		if (name.equals("id")) {
			return;
		}
		if (value instanceof Key) {
			entity.set(name, (Key) value);
		} else if (value instanceof Blob) {
			entity.set(name, (Blob) value);
		} else if (value instanceof String) {
			entity.set(name, (String) value);
		} else if (value instanceof Long) {
			if (name.endsWith("id")) {
				entity.set(name, (Long) value);
			} else {
				entity.set(name, Timestamp.of(new java.sql.Timestamp((Long) value)));
			}
		} else if (value instanceof Double) {
			entity.set(name, (Double) value);
		} else if (value instanceof Integer) {
			entity.set(name, (Integer) value);
		} else if (value instanceof LatLng) {
			entity.set(name, (LatLng) value);
		} else if (value instanceof Boolean) {
			entity.set(name, (Boolean) value);
		} else if (value instanceof FullEntity) {
			entity.set(name, (FullEntity<?>) value);
		} else if (value instanceof Enum) {
			entity.set(name, value.toString());
		} else {
			entity.set(name, value.toString());
		}
	}

	private <T extends GeneratedMessageV3> T convert(Class<?> c, Entity entity) {
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
				} else if (field.getJavaType() == JavaType.ENUM) {
					builder.setField(field, field.getEnumType().findValueByName(entity.getString(columnName)));
				} else if (field.getJavaType() == JavaType.LONG) {
					builder.setField(field, entity.getLong(columnName));
				} else if (field.getJavaType() == JavaType.BYTE_STRING) {
					builder.setField(field, ByteString.copyFrom(entity.getBlob(columnName).toByteArray()));
				} else if (field.getJavaType() == JavaType.STRING) {
					builder.setField(field, entity.getString(columnName));
				} else if (field.getJavaType() == JavaType.INT) {
					builder.setField(field, (int) entity.getLong(columnName));
				} else if (field.getJavaType() == JavaType.BOOLEAN) {
					builder.setField(field, entity.getBoolean(columnName));
				} else if (field.getJavaType() == JavaType.DOUBLE) {
					builder.setField(field, entity.getDouble(columnName));
				} else if (field.getJavaType() == JavaType.FLOAT) {
					builder.setField(field, (float) entity.getDouble(columnName));
				}
			} catch (Exception e) {
				System.out.println(e.getMessage() + " for column: " + columnName + ". value: " + entity.getValue(columnName));
			}
		}
		return (T) builder.build();
	}
}
