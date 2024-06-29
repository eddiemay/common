package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityServiceImpl<T,I> implements Createable<T>, Getable<T,I>, Listable<T>, Updateable<T,I>, Deleteable<T,I> {
	private final Store<T,I> store;
	private final LoginResolver loginResolver;

	public EntityServiceImpl(Store<T,I> store, LoginResolver loginResolver) {
		this.store = store;
		this.loginResolver = loginResolver;
	}

	protected Store<T, I> getStore() {
		return store;
	}

	protected LoginResolver getLoginResolver() {
		return loginResolver;
	}

	@Override
	public Class<T> getTypeClass() {
		return store.getTypeClass();
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "create")
	public T create(T entity, @Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken, "create");
			return transform(getStore().create(entity));
		} catch (DD4StorageException e) {
			e.printStackTrace();
			throw new ServiceException(e.getErrorCode(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "get")
	public T get(@Named("id") I id, @Nullable @Named("idToken") String idToken)
			throws ServiceException {
		try {
			resolveLogin(idToken, "get");
			return transform(getStore().get(id));
		} catch (DD4StorageException e) {
			e.printStackTrace();
			throw new ServiceException(e.getErrorCode(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "list")
	public QueryResult<T> list(
			@Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
			@Named("pageSize") @DefaultValue("200") int pageSize,
			@Named("pageToken") @DefaultValue("1") int pageToken,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken, "list");
			return transform(getStore().list(Query.forList(filter, orderBy, pageSize, pageToken)));
		} catch (DD4StorageException e) {
			e.printStackTrace();
			throw new ServiceException(e.getErrorCode(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "update")
	public T update(
			@Named("id") I id, T entity, @Named("updateMask") String updateMask,
			@Nullable @Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken, "update");
			return transform(getStore().update(id, current -> JSONUtil.merge(updateMask, entity, current)));
		} catch (DD4StorageException e) {
			e.printStackTrace();
			throw new ServiceException(e.getErrorCode(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
		}
	}

	@Override
	@ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "delete")
	public AtomicBoolean delete(@Named("id") I id, @Nullable @Named("idToken") String idToken)
			throws ServiceException {
		try {
			resolveLogin(idToken, "delete");
			return new AtomicBoolean(getStore().delete(id));
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
		}
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "migrate")
	public AtomicInteger migrate(@Named("idToken") String idToken) throws ServiceException {
		try {
			resolveLogin(idToken, "migrate");
			return new AtomicInteger(getStore().create(getStore().list(Query.forList()).getItems()).size());
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
		}
	}

	protected T transform(T t) {
		return transform(ImmutableList.of(t)).iterator().next();
	}

	protected QueryResult<T> transform(QueryResult<T> queryResult) {
		transform(queryResult.getItems());
		return queryResult;
	}

	protected Iterable<T> transform(Iterable<T> entities) {
		return entities;
	}

	protected boolean requiresLogin(String method) {
		return true;
	}

	protected void resolveLogin(String idToken, boolean requiresLogin) {
		loginResolver.resolve(idToken, requiresLogin);
	}

	protected void resolveLogin(String idToken, String method) {
		resolveLogin(idToken, requiresLogin(method));
	}
}
