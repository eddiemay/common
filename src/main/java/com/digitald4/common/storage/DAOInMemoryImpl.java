package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.util.List.of;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.json.JSONObject;

public class DAOInMemoryImpl implements DAO {

  private final AtomicLong idGenerator = new AtomicLong(5000);
  protected final Map<String, JSONObject> items = new HashMap<>();
  private final Map<Class<?>, List<JSONObject>> byType = new HashMap<>();

  @Override
  public <T> T create(T t) {
    JSONObject json = JSONUtil.toJSON(t);
    Object id = json.opt("id");
    if (id == null || id instanceof Number && (long) id == 0L) {
      json.put("id", id = idGenerator.incrementAndGet());
    }
    write(getIdString(t.getClass(), id), json);
    return t;
  }

  @Override
  public <T> ImmutableList<T> create(Iterable<T> entities) {
    return stream(entities).map(this::create).collect(toImmutableList());
  }

  @Override
  public <T, I> T get(Class<T> c, I id) {
    return JSONUtil.toObject(c, items.get(getIdString(c, id)));
  }

  @Override
  public <T, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids) {
    return BulkGetable.MultiListResult.of(stream(ids).map(id -> get(c, id)).collect(toImmutableList()), ids);
  }

  @Override
  public <T> QueryResult<T> list(Class<T> c, Query.List query) {
    ImmutableList<JSONObject> results = items.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(c.getSimpleName()))
        .map(Entry::getValue)
        .collect(toImmutableList());

    for (Filter filter : query.getFilters()) {
      String field = filter.getColumn();
      results = results.parallelStream()
          .filter(json -> {
            Object value = json.opt(field);
            Object filterValue = filter.getValue();
            switch (filter.getOperator()) {
              case "<": return ((Comparable) value).compareTo(filterValue) < 0;
              case "<=": return ((Comparable) value).compareTo(filterValue) <= 0;
              case ">=": return ((Comparable) value).compareTo(filterValue) >= 0;
              case ">": return ((Comparable) value).compareTo(filterValue) > 0;
            }
            return Objects.equals(value, filterValue);
          })
          .collect(toImmutableList());
    }

    for (OrderBy orderBy : query.getOrderBys()) {
      String field = orderBy.getColumn();
      results = results.stream()
          .sorted(
              (json1, json2) -> ((Comparable<Object>) json1.get(field)).compareTo(json2.get(field)))
          .collect(toImmutableList());
    }

    int totalSize = results.size();
    if (query.getOffset() > 0) {
      results = results.subList(query.getOffset(), results.size());
    }

    if (query.getLimit() != null && query.getLimit() > 0 && results.size() > query.getLimit()) {
      results = results.subList(0, query.getLimit());
    }

    return QueryResult.of(
        results.stream()
            .map(json -> JSONUtil.toObject(c, json)).collect(toImmutableList()), totalSize, query);
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
    throw new DD4StorageException("Unimplemented method", ErrorCode.BAD_REQUEST);
  }

  @Override
  public <T, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
    T t = get(c, id);
    if (t != null) {
      t = updater.apply(t);
      write(getIdString(c, id), JSONUtil.toJSON(t));
    }

    return t;
  }

  @Override
  public <T, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
    return stream(ids).map(id -> update(c, id, updater)).collect(toImmutableList());
  }

  @Override
  public <T, I> boolean delete(Class<T> c, I id) {
    return items.remove(getIdString(c, id)) != null;
  }

  @Override
  public <T, I> int delete(Class<T> c, Iterable<I> ids) {
    return (int) stream(ids).map(id -> delete(c, id)).filter(Boolean::booleanValue).count();
  }

  private <T> String getIdString(Class<T> c, Object id) {
    return String.format("%s-%s", c.getSimpleName(), id);
  }

  protected void write(String id, JSONObject json) {
    items.put(id, json);
  }
}
