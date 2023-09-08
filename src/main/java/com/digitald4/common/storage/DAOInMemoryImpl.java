package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.json.JSONObject;

public class DAOInMemoryImpl implements DAO {

  private final AtomicLong idGenerator = new AtomicLong(5000);
  private final Map<String, JSONObject> items = new HashMap<>();

  @Override
  public <T> T create(T t) {
    JSONObject json = JSONUtil.toJSON(t);
    Object id = json.opt("id");
    if (id == null || id instanceof Number && (long) id == 0L) {
      json.put("id", id = idGenerator.incrementAndGet());
    }
    items.put(getIdString(t.getClass(), id), json);
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
  public <T, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
    return stream(ids).map(id -> get(c, id)).filter(Objects::nonNull).collect(toImmutableList());
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
              case "<": return (Integer) value < (Integer) filterValue;
              case "<=": return (Integer) value <= (Integer) filterValue;
              case ">=": return (Integer) value >= (Integer) filterValue;
              case ">": return (Integer) value > (Integer) filterValue;
            }
            return filterValue.equals(value);
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
      items.put(getIdString(c, id), JSONUtil.toJSON(t));
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

  public DAOInMemoryImpl loadFromFile(String file) {
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null && !line.isEmpty()) {
        JSONObject entry = new JSONObject(line);
        items.put(entry.getString("idString"), entry.getJSONObject("entity"));
      }
    } catch (FileNotFoundException fnfe) {
      System.out.println("Load file not found, continuing");
    } catch (IOException ioe) {
      throw new DD4StorageException("Error reading load file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
    }

    return this;
  }

  public DAOInMemoryImpl saveToFile(String file) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
      items.entrySet().stream()
          .sorted(Entry.comparingByKey())
          .map(e -> new JSONObject().put("idString", e.getKey()).put("entity", e.getValue()))
          .forEach(json -> {
            try {
              bw.write(json + "\n");
            } catch (IOException ioe) {
              throw new DD4StorageException("Error writting file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
            }
          });
    } catch (IOException ioe) {
      throw new DD4StorageException("Error writting file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
    }
    return this;
  }

  private <T> String getIdString(Class<T> c, Object id) {
    return String.format("%s-%s", c.getSimpleName(), id);
  }
}
