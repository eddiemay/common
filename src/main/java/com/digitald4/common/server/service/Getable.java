package com.digitald4.common.server.service;

import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;

public interface Getable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "id/{id}")
  T get(@Named("id") long id);
}
