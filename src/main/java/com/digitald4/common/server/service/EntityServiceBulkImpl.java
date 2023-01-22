package com.digitald4.common.server.service;

import static com.google.common.collect.Streams.stream;
import static java.util.function.Function.identity;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


public class EntityServiceBulkImpl<I, T extends ModelObject<I>> extends EntityServiceImpl<T, I>
    implements BulkCreateable<T>, BulkGetable<T,I>, BulkUpdateable<T,I>, BulkDeleteable<T,I> {

  public EntityServiceBulkImpl(Store<T,I> store, LoginResolver loginResolver, boolean requiresLoginDefault) {
    super(store, loginResolver, requiresLoginDefault);
  }

  public EntityServiceBulkImpl(Store<T,I> store, LoginResolver loginResolver) {
    super(store, loginResolver);
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "batchCreate")
  public ImmutableList<T> batchCreate(Iterable<T> entities, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken,"batchCreate");
      return getStore().create(entities);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "batchGet")
  public ImmutableList<T> batchGet(Iterable<I> ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken,"batchGet");
      return getStore().get(ids);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "batchUpdate")
  public ImmutableList<T> batchUpdate(
      Iterable<T> entities, @Named("updateMask") String updateMask, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken,"batchUpdate");
      ImmutableMap<I, T> entityMap = stream(entities).collect(toImmutableMap(T::getId, identity()));
      return getStore().update(
          entityMap.keySet(), current -> JSONUtil.merge(updateMask, entityMap.get(current.getId()), current));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "batchDelete")
  public Empty batchDelete(Iterable<I> ids, @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken,"batchDelete");
      getStore().delete(ids);
      return Empty.getInstance();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
