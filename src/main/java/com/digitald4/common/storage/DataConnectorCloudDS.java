package com.digitald4.common.storage;

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
import java.util.Map;
import java.util.function.UnaryOperator;

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
	public <T extends GeneratedMessageV3> T get(Class<T> c, int id) {
		return new RetryableFunction<Integer, T>() {
			@Override
			public T apply(Integer id) {
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
					Filter filter = request.getFilter(0);
					query.setFilter(PropertyFilter.eq(filter.getColumn(), filter.getValue()));
				}
				request.getOrderByList().forEach(orderBy -> query.addOrderBy(orderBy.getDesc()
						? OrderBy.desc(orderBy.getColumn()) : OrderBy.asc(orderBy.getColumn())));
				ListResponse.Builder<T> listResponse = ListResponse.newBuilder();
				datastore.run(query.build()).forEachRemaining(entity -> listResponse.addResult(convert(c, entity)));
				return listResponse.build();
			}
		}.apply(listRequest);
	}

	@Override
	public <T extends GeneratedMessageV3> T update(Class<T> c, int id, UnaryOperator<T> updater) {
		return new RetryableFunction<Pair<Integer, UnaryOperator<T>>, T>() {
			@Override
			public T apply(Pair<Integer, UnaryOperator<T>> pair) {
				T t = updater.apply(get(c, id));
				Entity.Builder entity = Entity.newBuilder(getKeyFactory(c).newKey(id));
				t.getAllFields()
						.forEach((key, value) -> set(entity, key.getName(), value));
				return convert(c, datastore.put(entity.build()));
			}
		}.apply(Pair.of(id, updater));
	}

	@Override
	public <T> void delete(Class<T> c, int id) {
		new RetryableFunction<Integer, Boolean>() {
			@Override
			public Boolean apply(Integer id) {
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
		if (value instanceof Key) {
			entity.set(name, (Key) value);
		} else if (value instanceof Blob) {
			entity.set(name, (Blob) value);
		} else if (value instanceof String) {
			entity.set(name, (String) value);
		} else if (value instanceof Long) {
			entity.set(name, Timestamp.of(new java.sql.Timestamp((Long) value)));
		} else if (value instanceof Double) {
			entity.set(name, (Double) value);
		} else if (value instanceof LatLng) {
			entity.set(name, (LatLng) value);
		} else if (value instanceof Boolean) {
			entity.set(name, (Boolean) value);
		} else if (value instanceof FullEntity) {
			entity.set(name, (FullEntity<?>) value);
		} else {
			entity.set(name, value.toString());
		}
	}

	private <T extends GeneratedMessageV3> T convert(Class<?> c, Entity entity) {
		Message.Builder builder = getDefaultInstance(c).toBuilder();
		entity.getNames();
		Descriptor descriptor = builder.getDescriptorForType();
		builder.setField(descriptor.findFieldByName("id"), entity.getKey().getId().intValue());
		for (String columnName : entity.getNames()) {
			Object value = entity.getValue(columnName);
			if (value != null) {
				try {
					FieldDescriptor field = descriptor.findFieldByName(columnName);
					if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
						JsonFormat.parser().ignoringUnknownFields()
								.merge("{\"" + field.getName() + "\": " + entity.getString(columnName) + "}", builder);
					} else if (field.getJavaType() == JavaType.ENUM) {
						value = field.getEnumType().findValueByNumber((int) entity.getLong(columnName));
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.LONG) {
						value = entity.getLong(columnName);
						builder.setField(field, value);
					} else if (field.getJavaType() == JavaType.BYTE_STRING) {
						builder.setField(field, ByteString.copyFrom(entity.getBlob(columnName).toByteArray()));
					} else {
						builder.setField(field, value);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage() + " for column: " + columnName + ". value: " + value);
				}
			}
		}
		return (T) builder.build();
	}
}
