package com.digitald4.common.server.service;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.model.UpdateRequest;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteResponse;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.ProtoUtil;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;

public class SingleProtoService<T> implements EntityService<T> {

	private final Store<T> store;

	public SingleProtoService(Store<T> store) {
		this.store = store;
	}

	protected T getType() {
		return store.getType();
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "_")
	public T create(T entity) {
		return store.create(entity);
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "{id}")
	public T get(@Named("id") long id) {
		return store.get(id);
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
	public QueryResult<T> list(ListRequest request) {
		return store.list(ProtoUtil.toQuery(request));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(@Named("id") long id, UpdateRequest<T> updateRequest) {
		T entity = updateRequest.getEntity();
		if (entity instanceof Message) {
			return store.update(id, internal ->
					(T) ProtoUtil.merge(updateRequest.getUpdateMask(), (Message) entity, (Message) internal));
		} else if (entity instanceof HasProto) {
			return (T) store.update(id, internal ->
					(T) ProtoUtil.merge(updateRequest.getUpdateMask(), (HasProto) entity, (HasProto) internal));
		}
		throw new IllegalArgumentException("Can not update type: " + entity.getClass());
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
}
