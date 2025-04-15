package com.digitald4.common.server.service;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Streams.stream;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.json.JSONObject;

public interface BulkGetable<T,I> extends EntityService<T> {
  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET)
  MultiListResult<T, I> batchGet(String ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException;

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
  MultiListResult<T, I> bulkGet(Ids ids, @Nullable @Named("idToken") String idToken)
      throws ServiceException;

  class Ids {
    private ImmutableList<String> items = ImmutableList.of();

    public ImmutableList<String> getItems() {
      return items;
    }

    public ImmutableList<Long> itemsAsLongs() {
      return items.stream().map(Long::parseLong).collect(toImmutableList());
    }

    public Ids setItems(Iterable<String> items) {
      this.items = ImmutableList.copyOf(items);
      return this;
    }
  }

  class MultiListResult<T, I> {
    private final ImmutableList<T> items;
    private final ImmutableList<I> requestedIds;
    private final ImmutableList<I> missingIds;
    private final Class<?> idClass;

    private MultiListResult(Iterable<T> items, Iterable<I> requestedIds, Iterable<I> missingIds) {
      this.items = ImmutableList.copyOf(items);
      this.requestedIds = ImmutableList.copyOf(requestedIds);
      this.missingIds = ImmutableList.copyOf(missingIds);
      idClass = requestedIds.iterator().hasNext() ? requestedIds.iterator().next().getClass() : null;
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

    public Class<?> getIdClass() {
      return idClass;
    }

    public static <T, I> MultiListResult<T, I> of(Iterable<T> items, Iterable<I> requestedIds) {
      if (size(items) == size(requestedIds)) {
        return new MultiListResult<>(items, requestedIds, ImmutableList.of());
      }

      ImmutableSet<I> foundIds =
          stream(items).map(JSONObject::new).map(json -> (I) json.get("id")).collect(toImmutableSet());
      return new MultiListResult<>(
          items, requestedIds, stream(requestedIds).filter(id -> !foundIds.contains(id)).collect(toImmutableSet()));
    }
  }
}
