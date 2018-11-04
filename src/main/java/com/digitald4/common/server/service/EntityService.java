package com.digitald4.common.server.service;

import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteResponse;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.model.UpdateRequest;
import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.protobuf.Empty;

public interface EntityService<T> {

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
	T create(T entity);

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET)
	T get(@Named("id") long id);

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET)
	QueryResult<T> list(ListRequest request);

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT)
	T update(@Named("id") long id, UpdateRequest<T> request);

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE)
	Empty delete(@Named("id") long id);

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
	BatchDeleteResponse batchDelete(BatchDeleteRequest request);
}
