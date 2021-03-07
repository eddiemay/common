package com.digitald4.common.server.service;

import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;

public interface Updateable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
  T update(@Named("id") long id, T entity, @Named("updateMask") String updateMask);
}
