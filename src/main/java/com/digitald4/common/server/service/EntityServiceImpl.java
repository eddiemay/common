package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public class EntityServiceImpl<T, U extends User>
		implements Createable<T>, Getable<T>, Listable<T>, Updateable<T>, Deleteable<T> {

	private final Store<T> store;
	private final SessionStore<U> sessionStore;
	private final boolean requiresLoginDefault;

	public EntityServiceImpl(Store<T> store, SessionStore<U> sessionStore, boolean requiresLoginDefault) {
		this.store = store;
		this.sessionStore = sessionStore;
		this.requiresLoginDefault = requiresLoginDefault;
	}

	protected Store<T> getStore() {
		return store;
	}

	@Override
	public T getType() {
		return store.getType();
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "_")
	public T create(T entity, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("create"));
			return store.create(entity);
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "{id}")
	public T get(@Named("id") long id, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("get"));
			return store.get(id);
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
	public QueryResult<T> list(
			@Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
			@Named("pageSize") @DefaultValue("0") int pageSize, @Named("pageToken") @DefaultValue("0") int pageToken,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("list"));
			return store.list(Query.forValues(filter, orderBy, pageSize, pageToken));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(
			@Named("id") long id, T entity, @Named("updateMask") String updateMask,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("update"));
			return store.update(id, current -> JSONUtil.merge(updateMask, entity, current));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "{id}")
	public Empty delete(@Named("id") long id, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			sessionStore.resolve(idToken, requiresLogin("delete"));
			store.delete(id);
			return Empty.getInstance();
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	protected boolean requiresLogin(String method) {
		return requiresLoginDefault;
	}
}