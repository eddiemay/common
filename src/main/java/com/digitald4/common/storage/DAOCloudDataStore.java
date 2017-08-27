package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;
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
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class DAOCloudDataStore<T extends GeneratedMessageV3> implements DAO<T> {

	private final T type;
	private final Descriptor descriptor;
	private final Datastore datastore;
	private final KeyFactory keyFactory;

	public DAOCloudDataStore(Class<T> c) {
		try {
			this.type = (T) c.getMethod("getDefaultInstance").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		this.descriptor = type.getDescriptorForType();
		this.datastore = DatastoreOptions.getDefaultInstance().getService();
		this.keyFactory = datastore.newKeyFactory().setKind(c.getSimpleName());
	}

	@Override
	public T getType() {
		return type;
	}

	@Override
	public T create(T t) {
		Entity.Builder entity = Entity.newBuilder(datastore.allocateId(keyFactory.newKey()));
		t.getAllFields()
				.forEach((key, value) -> set(entity, key.getName(), value));
		return toT.apply(datastore.put(entity.build()));
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
		}
	}

	@Override
	public T get(int id) {
		return toT.apply(datastore.get(keyFactory.newKey(id)));
	}

	@Override
	public ListResponse<T> list(ListRequest request) {
		EntityQuery.Builder query = Query.newEntityQueryBuilder()
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
		datastore.run(query.build()).forEachRemaining(entity -> listResponse.addResult(toT.apply(entity)));
		return listResponse.build();
	}

	@Override
	public T update(int id, UnaryOperator<T> updater) {
		return toT.apply(datastore.put(toEntity.apply(updater.apply(get(id)))));
	}

	@Override
	public void delete(int id) {
		datastore.delete(keyFactory.newKey(id));
	}

	private Descriptor getDescriptor(){
		return descriptor;
	}

	private Function<T, FullEntity> toEntity = t -> {
		FullEntity.Builder entity = Entity.newBuilder();
		t.getAllFields()
				.forEach((key, value) -> entity.set(key.getName(), String.valueOf(value)));
		return entity.build();
	};

	private Function<Entity, T> toT = entity -> {
		Message.Builder builder = getType().newBuilderForType();
		entity.getNames();
		for (String columnName : entity.getNames()) {
			Object value = entity.getValue(columnName);
			if (value != null) {
				try {
					FieldDescriptor field = getDescriptor().findFieldByName(columnName);
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
	};
}
