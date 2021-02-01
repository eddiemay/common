package com.digitald4.common.server.service;

import com.google.api.server.spi.config.ApiMethod;

public interface Createable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  T create(T entity);
}
