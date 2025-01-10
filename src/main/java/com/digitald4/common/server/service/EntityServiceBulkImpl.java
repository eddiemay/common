package com.digitald4.common.server.service;

import static java.util.function.Function.identity;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityServiceBulkImpl<I, T extends ModelObject<I>> extends EntityServiceImpl<T, I>
    implements /*BulkCreateable<T>,*/ BulkGetable<T,I>, BulkUpdateable<T,I>, BulkDeleteable<T,I> {


  public EntityServiceBulkImpl(Store<T,I> store, LoginResolver loginResolver) {
    super(store, loginResolver);
  }

  /* @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "batchCreate")
  public ImmutableList<T> batchCreate(
      IterableParam<T> entities, @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken,"batchCreate");
      return getStore().create(entities.getItems());
    } catch (DD4StorageException e) {
      e.printStackTrace();
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  } */

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "batchGet")
  public MultiListResult<T, I> batchGet(IterableParam<I> ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken, "batchGet");
      return getStore().get((Iterable<I>)
          ids.getItems().stream().map(id -> Long.parseLong(id.toString())).collect(toImmutableList()));
    } catch (DD4StorageException e) {
      e.printStackTrace();
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "batchUpdate")
  public ImmutableList<T> batchUpdate(
      IterableParam<T> entities, @Named("updateMask") String updateMask, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken,"batchUpdate");
      ImmutableMap<I, T> entityMap = entities.getItems().stream().collect(toImmutableMap(T::getId, identity()));
      return getStore().update(
          entityMap.keySet(), current -> JSONUtil.merge(updateMask, entityMap.get(current.getId()), current));
    } catch (DD4StorageException e) {
      e.printStackTrace();
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @Override
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "batchDelete")
  public AtomicInteger batchDelete(IterableParam<I> ids, @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken, "batchDelete");
      return new AtomicInteger(getStore().delete(ids.getItems()));
    } catch (DD4StorageException e) {
      e.printStackTrace();
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }
}
