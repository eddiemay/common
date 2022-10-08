package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public class EntityServiceImpl<T,I>
		implements Createable<T>, Getable<T,I>, Listable<T>, Updateable<T,I>, Deleteable<T,I> {

	private final Store<T,I> store;
	private final LoginResolver loginResolver;
	private final boolean requiresLoginDefault;

	public EntityServiceImpl(Store<T,I> store, LoginResolver loginResolver, boolean requiresLoginDefault) {
		this.store = store;
		this.loginResolver = loginResolver;
		this.requiresLoginDefault = requiresLoginDefault;
	}

	public EntityServiceImpl(Store<T,I> store, LoginResolver loginResolver) {
		this(store, loginResolver, true);
	}

	protected Store<T,I> getStore() {
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
			resolveLogin(idToken, "create");
			return getStore().create(entity);
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "{id}")
	public T get(@Named("id") I id, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken,"get");
			return getStore().get(id);
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "_")
	public QueryResult<T> list(
			@Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
			@Named("pageSize") @DefaultValue("200") int pageSize, @Named("pageToken") @DefaultValue("1") int pageToken,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken,"list");
			return getStore().list(Query.forList(filter, orderBy, pageSize, pageToken));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
	public T update(
			@Named("id") I id, T entity, @Named("updateMask") String updateMask,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken,"update");
			return getStore().update(id, current -> JSONUtil.merge(updateMask, entity, current));
		} catch (DD4StorageException e) {
			e.printStackTrace();
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "{id}")
	public Empty delete(@Named("id") I id, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken,"delete");
			getStore().delete(id);
			return Empty.getInstance();
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	protected boolean requiresLogin(String method) {
		return requiresLoginDefault;
	}

	protected void resolveLogin(String idToken, boolean requiresLogin) {
		loginResolver.resolve(idToken, requiresLogin);
	}

	protected void resolveLogin(String idToken, String method) {
		resolveLogin(idToken, requiresLogin(method));
	}
}
