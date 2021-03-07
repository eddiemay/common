package com.digitald4.common.server.service;

import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public interface BulkDeleteable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  BatchDeleteResponse batchDelete(@Named("id") Iterable<Long> ids);
}
