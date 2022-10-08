package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public interface Updateable<T,I> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.PUT, path = "{id}")
  T update(@Named("id") I id, T entity, @Named("updateMask") String updateMask,
           @Nullable @Named("idToken") String idToken) throws ServiceException;
}
