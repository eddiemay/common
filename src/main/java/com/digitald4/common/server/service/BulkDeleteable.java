package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public interface BulkDeleteable<T,I> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  Empty batchDelete(Iterable<I> ids, @Nullable @Named("idToken") String idToken) throws ServiceException;
}
