package com.digitald4.common.server.service;

import com.digitald4.common.server.service.BulkGetable.Ids;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public interface BulkDeleteable<T,I> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  AtomicInteger batchDelete(Ids ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException;
}
