package com.digitald4.common.server.service;

import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteResponse;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.model.UpdateRequest;
import com.digitald4.common.util.ProtoUtil;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DualProtoService<T extends Message, I extends Message> implements EntityService<T> {

	private final Class<T> cls;
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
						switch (field.getJavaType()) {
							case ENUM:
								builder.setField(field, field.getEnumType()
										.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
								break;
							default:
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
					switch (field.getJavaType()) {
						case ENUM:
							builder.setField(field, field.getEnumType()
									.findValueByNumber(((Descriptors.EnumValueDescriptor) entry.getValue()).getNumber()));
							break;
						default: builder.setField(field, entry.getValue());
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
		this.cls = (Class<T>) type.getClass();
		this.type = type;
		this.externalDescriptor = type.getDescriptorForType();
		this.store = store;
		this.internalDescriptor = store.getType().getDescriptorForType();
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
	public QueryResult<T> list(ListRequest request) {
		return toListResponse(store.list(ProtoUtil.toQuery(request)));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(@Named("id") long id, UpdateRequest<T> updateRequest) {
		return getConverter().apply(store.update(id, internal -> ProtoUtil.merge(
				updateRequest.getUpdateMask(), getReverseConverter().apply(updateRequest.getEntity()), internal)));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "{id}")
	public Empty delete(@Named("id") long id) {
		store.delete(id);
		return Empty.getDefaultInstance();
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE)
	public BatchDeleteResponse batchDelete(BatchDeleteRequest request) {
		return BatchDeleteResponse.newBuilder()
				.setDeleted(store.delete(ProtoUtil.toQuery(request)))
				.build();
	}

	protected QueryResult<T> toListResponse(QueryResult<I> queryResult) {
		return new QueryResult<>(
				queryResult.getResults().stream()
						.map(getConverter())
						.collect(Collectors.toList()),
				queryResult.getTotalSize());
	}
}
