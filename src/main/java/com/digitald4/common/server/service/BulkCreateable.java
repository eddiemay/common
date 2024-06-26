package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;

public interface BulkCreateable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  ImmutableList<T> batchCreate(IterableParam<T> entites, @Nullable @Named("idToken") String idToken) throws ServiceException;
}
