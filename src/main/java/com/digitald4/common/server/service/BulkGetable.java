package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;

public interface BulkGetable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  ImmutableList<T> batchGet(Iterable<Long> ids, @Nullable @Named("idToken") String idToken) throws ServiceException;
}
