package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.json.JSONObject;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Streams.stream;

public interface BulkGetable<T,I> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  MultiListResult<T, I> batchGet(IterableParam<I> ids, @Nullable @Named("idToken") String idToken) throws ServiceException;

  class MultiListResult<T, I> {
    private final ImmutableList<T> items;
    private final ImmutableList<I> requestedIds;
    private final ImmutableList<I> missingIds;

    private MultiListResult(Iterable<T> items, Iterable<I> requestedIds, Iterable<I> missingIds) {
      this.items = ImmutableList.copyOf(items);
      this.requestedIds = ImmutableList.copyOf(requestedIds);
      this.missingIds = ImmutableList.copyOf(missingIds);
    }

    public ImmutableList<T> getItems() {
      return items;
    }

    public ImmutableList<I> getRequestedIds() {
      return requestedIds;
    }

    public ImmutableList<I> getMissingIds() {
      return missingIds;
    }

    public static <T, I> MultiListResult<T, I> of(Iterable<T> items, Iterable<I> requestedIds) {
      if (size(items) == size(requestedIds)) {
        return new MultiListResult<>(items, requestedIds, ImmutableList.of());
      }

      ImmutableSet<I> foundIds =
          stream(items).map(JSONObject::new).map(json -> (I) json.get("id")).collect(toImmutableSet());
      return new MultiListResult<>(
          items, requestedIds, stream(requestedIds).filter(foundIds::contains).collect(toImmutableSet()));
    }
  }
}
