package com.digitald4.common.server.service;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;

public class EntityServiceBulkImpl<T> extends EntityServiceImpl<T>
    implements BulkCreateable<T>, BulkGetable<T>, BulkUpdateable<T>, BulkDeleteable<T> {

  private final Store<T> store;
  private final LoginResolver loginResolver;
  private final boolean requiresLoginDefault;

  public EntityServiceBulkImpl(Store<T> store, LoginResolver loginResolver, boolean requiresLoginDefault) {
    super(store, loginResolver, requiresLoginDefault);
    this.store = store;
    this.loginResolver = loginResolver;
    this.requiresLoginDefault = requiresLoginDefault;
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  public ImmutableList<T> batchCreate(Iterable<T> entites, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      loginResolver.resolve(idToken, requiresLogin("batchCreate"));
      return store.create(entites);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  public ImmutableList<T> batchGet(Iterable<Long> ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      loginResolver.resolve(idToken, requiresLogin("batchGet"));
      return store.get(ids);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT)
  public ImmutableList<T> batchUpdate(
      @Named("ids") Iterable<Long> ids, T entity, @Named("updateMask") String updateMask,
      @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      loginResolver.resolve(idToken, requiresLogin("batchUpdate"));
      return store.update(ids, current -> JSONUtil.merge(updateMask, entity, current));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  public Empty batchDelete(Iterable<Long> ids, @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      loginResolver.resolve(idToken, requiresLogin("batchDelete"));
      store.delete(ids);
      return Empty.getInstance();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  protected boolean requiresLogin(String method) {
    return requiresLoginDefault;
  }
}
