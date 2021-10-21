package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public interface Deleteable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.DELETE, path = "id/{id}")
  Empty delete(@Named("id") long id, @Nullable @Named("idToken") String idToken) throws ServiceException;
}
