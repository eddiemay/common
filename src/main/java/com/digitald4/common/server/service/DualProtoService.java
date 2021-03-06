package com.digitald4.common.server.service;

import com.digitald4.common.storage.Store;
import com.digitald4.common.storage.Query;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DualProtoService<T extends Message, I extends Message>
		implements Createable<T>, Getable<T>, Listable<T>, Updateable<T>, Deleteable<T> {

	private final T type;
	private final Store<I> store;
	private final Descriptor internalDescriptor;
	private final Descriptor externalDescriptor;

	private final Function<I, T> converter = new Function<I, T>() {
		@Override
		public T apply(I internal) {
			Message.Builder builder = type.newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : internal.getAllFields().entrySet()) {
				FieldDescriptor field = externalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					try {
						if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
							builder.setField(field, field.getEnumType()
									.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
						} else {
							builder.setField(field, entry.getValue());
						}
					} catch (IllegalArgumentException iae) {
						throw new IllegalArgumentException("for field: " + field + " value: " + entry.getValue(), iae);
					}
				}
			}
			return (T) builder.build();
		}
	};
	
	private final Function<T, I> reverse = new Function<T, I>() {
		@Override
		public I apply(T external) {
			Message.Builder builder = store.getType().newBuilderForType();
			for (Map.Entry<FieldDescriptor, Object> entry : external.getAllFields().entrySet()) {
				FieldDescriptor field = internalDescriptor.findFieldByName(entry.getKey().getName());
				if (field != null) {
					if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
						builder.setField(field, field.getEnumType()
								.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
					} else {
						builder.setField(field, entry.getValue());
					}
				}
			}
			return (I) builder.build();
		}
	};
	
	public DualProtoService(Class<T> c, Store<I> store) {
		this(ProtoUtil.getDefaultInstance(c), store);
	}

	protected DualProtoService(T type, Store<I> store) {
		this.type = type;
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();
	}

	@Override
	public Store<T> getStore() {
		return null;
	}

	protected T getType() {
		return type;
	}

	protected Function<I, T> getConverter() {
		return converter;
	}

	protected Function<T, I> getReverseConverter() {
		return reverse;
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "_")
	public T create(T entity) {
		return getConverter().apply(store.create(getReverseConverter().apply(entity)));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "{id}")
	public T get(@Named("id") long id) {
		return getConverter().apply(store.get(id));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
	public QueryResult<T> list(
			@Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
			@Named("pageSize") @DefaultValue("0") int pageSize, @Named("pageToken") @DefaultValue("0") int pageToken) {
		return toListResponse(store.list(Query.forValues(filter, orderBy, pageSize, pageToken)));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(@Named("id") long id, T entity, @Named("updateMask") String updateMask) {
		return getConverter().apply(
				store.update(id, internal -> ProtoUtil.merge(updateMask, getReverseConverter().apply(entity), internal)));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "id/{id}")
	public Empty delete(@Named("id") long id) {
		getStore().delete(id);
		return Empty.getInstance();
	}

	protected QueryResult<T> toListResponse(QueryResult<I> queryResult) {
		return new QueryResult<>(
				queryResult.getResults().stream().map(getConverter()).collect(Collectors.toList()),
				queryResult.getTotalSize());
	}
}
