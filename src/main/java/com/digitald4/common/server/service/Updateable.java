package com.digitald4.common.server.service;

import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;

public interface Updateable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT)
  T update(@Named("id") long id, UpdateRequest<T> updateRequest);
}
