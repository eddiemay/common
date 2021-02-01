package com.digitald4.common.server.service;

import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;

public interface BulkDeleteable<T> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  BatchDeleteResponse batchDelete(
      @Nullable @Named("filter") String filter, @Nullable @Named("orderBy") String orderBy,
      @Named("pageSize") @DefaultValue("0") int pageSize, @Named("pageToken") @DefaultValue("0") int pageToken);
}
