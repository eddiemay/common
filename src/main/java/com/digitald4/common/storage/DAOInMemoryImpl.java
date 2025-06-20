package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

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
import org.json.JSONObject;

public class DAOInMemoryImpl implements DAO {
  private final AtomicLong idGenerator = new AtomicLong(5000);
  protected final Map<String, JSONObject> items = new HashMap<>();
  private final ChangeTracker changeTracker;

  public DAOInMemoryImpl(ChangeTracker changeTracker) {
    this.changeTracker = changeTracker;
  }

  @Override
  public <T> Transaction<T> persist(Transaction<T> transaction) {
    changeTracker.prePersist(this, transaction);
    transaction.getOps().forEach(op -> {
      JSONObject json = JSONUtil.toJSON(op.getEntity());
      Object id = json.opt("id");
        if (id == null || id instanceof Number && (long) id == 0L) {
          json.put("id", id = idGenerator.incrementAndGet());
        }
        write(getIdString(op.getEntity().getClass(), id), json);
        op.setId(id);
      });
    changeTracker.postPersist(this, transaction);
    return transaction;
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
            if (value == null) {
              return false;
            }
            Object filterValue = filter.getValue();
            return switch (filter.getOperator()) {
              case "<" -> ((Comparable) value).compareTo(filterValue) < 0;
              case "<=" -> ((Comparable) value).compareTo(filterValue) <= 0;
              case ">=" -> ((Comparable) value).compareTo(filterValue) >= 0;
              case ">" -> ((Comparable) value).compareTo(filterValue) > 0;
              default -> Objects.equals(value, filterValue);
            };
          })
          .collect(toImmutableList());
    }

    for (OrderBy orderBy : query.getOrderBys()) {
      String field = orderBy.getColumn();
      results = results.stream()
          .sorted((json1, json2) -> ((Comparable<Object>) json1.get(field)).compareTo(json2.get(field)))
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
        c, results.stream().map(json -> JSONUtil.toObject(c, json)).collect(toImmutableList()), totalSize, query);
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
    throw new DD4StorageException("Unimplemented method", ErrorCode.BAD_REQUEST);
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
