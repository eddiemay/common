package com.digitald4.common.server.service;

import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.ProtoUtil;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.protobuf.Message;

import java.util.Arrays;

public class EntityServiceImpl<T> implements Createable<T>, Getable<T>, Listable<T>, Updateable<T>, Deleteable<T> {

	private final Store<T> store;
	public EntityServiceImpl(Store<T> store) {
		this.store = store;
	}

	@Override
	public Store<T> getStore() {
		return store;
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "_")
	public T create(T entity) {
		return getStore().create(entity);
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "{id}")
	public T get(@Named("id") long id) {
		return getStore().get(id);
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
	public QueryResult<T> list(
			@Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
			@Named("pageSize") @DefaultValue("0") int pageSize, @Named("pageToken") @DefaultValue("0") int pageToken) {
		return getStore().list(Query.forValues(filter, orderBy, pageSize, pageToken));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(@Named("id") long id, T entity, @Named("updateMask") String updateMask) {
		return getStore().update(id, internal -> ProtoUtil.merge(updateMask, entity, internal));
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "{id}")
	public Empty delete(@Named("id") long id) {
		getStore().delete(id);
		return Empty.getInstance();
	}
}
