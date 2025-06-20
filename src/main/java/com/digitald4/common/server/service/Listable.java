package com.digitald4.common.server.service;

import com.digitald4.common.storage.QueryResult;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public interface Listable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "list")
  QueryResult<T> list(
      @Nullable @Named("fields") String fields, @Nullable @Named("filter") String filter,
      @Nullable @Named("orderBy") String orderBy,
      @Named("pageSize") @DefaultValue("0") int pageSize, @Named("pageToken") @DefaultValue("0") int pageToken,
      @Nullable @Named("idToken") String idToken) throws ServiceException;
}
