package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;

public interface BulkUpdateable<T,I> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT)
  ImmutableList<T> batchUpdate(IterableParam<T> entities, @Named("updateMask") String updateMask,
      @Nullable @Named("idToken") String idToken) throws ServiceException;
}
