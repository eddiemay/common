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

  public EntityServiceBulkImpl(Store<T> store, LoginResolver loginResolver, boolean requiresLoginDefault) {
    super(store, loginResolver, requiresLoginDefault);
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  public ImmutableList<T> batchCreate(Iterable<T> entites, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken,"batchCreate");
      return getStore().create(entites);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  public ImmutableList<T> batchGet(Iterable<Long> ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken,"batchGet");
      return getStore().get(ids);
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
      resolveLogin(idToken,"batchUpdate");
      return getStore().update(ids, current -> JSONUtil.merge(updateMask, entity, current));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  public Empty batchDelete(Iterable<Long> ids, @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken,"batchDelete");
      getStore().delete(ids);
      return Empty.getInstance();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
