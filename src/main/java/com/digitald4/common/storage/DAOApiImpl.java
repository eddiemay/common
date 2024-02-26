package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.google.common.collect.Streams;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

public class DAOApiImpl implements DAO {
  private final APIConnector apiConnector;

  public DAOApiImpl(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public <T> T create(T t) {
    String url = apiConnector.formatUrl(getResourceName(t.getClass())) + "/create";
    return convert((Class<T>) t.getClass(), apiConnector.sendPost(url, new JSONObject(t).toString()));
  }

  @Override
  public <T> ImmutableList<T> create(Iterable<T> entities) {
    Class<T> c = (Class<T>) entities.iterator().next().getClass();
    String url = apiConnector.formatUrl(getResourceName(c)) + "/batchCreate";
    JSONArray items = new JSONArray();
    Streams.stream(entities).map(JSONObject::new).forEach(items::put);
    JSONObject postData = new JSONObject().put("items", items);
    return convertList(c, apiConnector.sendPost(url, postData.toString()));
  }

  @Override
  public <T, I> T get(Class<T> c, I id) {
    String url = apiConnector.formatUrl(getResourceName(c)) + "/get?id=" + id;
    return convert(c, apiConnector.sendGet(url));
  }

  @Override
  public <T, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
    String url = apiConnector.formatUrl(getResourceName(c)) + "/batchGet";
    return convertList(c, apiConnector.sendPost(url, new JSONObject().put("items", ids).toString()));
  }

  @Override
  public <T> QueryResult<T> list(Class<T> c, Query.List query) {
    StringBuilder url = new StringBuilder(apiConnector.formatUrl(getResourceName(c)) + "/list");

    ImmutableList.Builder<String> parameters = ImmutableList.builder();
    if (!query.getFilters().isEmpty()) {
      parameters.add("filter=" + query.getFilters().stream()
          .map(filter -> filter.getColumn() + filter.getOperator() + filter.getValue())
          .collect(Collectors.joining(",")));
    }
    if (!query.getOrderBys().isEmpty()) {
      parameters.add("orderBy=" + query.getOrderBys().stream()
          .map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
          .collect(Collectors.joining(",")));
    }
    if (query.getPageSize() != null) {
      parameters.add("pageSize=" + query.getPageSize());
    }
    if (query.getPageToken() > 0) {
      parameters.add("pageToken=" + query.getPageToken());
    }

    if (!parameters.build().isEmpty()) {
      url.append(parameters.build().stream().collect(Collectors.joining("&", "?", "")));
    }
    JSONObject response = new JSONObject(apiConnector.sendGet(url.toString()));

    int totalSize = response.getInt("totalSize");
    if (totalSize == 0) {
      return QueryResult.of(ImmutableList.of(), totalSize, query);
    }

    JSONArray resultArray = response.getJSONArray("items");
    ImmutableList<T> results = IntStream.range(0, resultArray.length())
        .mapToObj(x -> convert(c, resultArray.getJSONObject(x).toString()))
        .collect(toImmutableList());

    return QueryResult.of(results, totalSize, query);
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Search query) {
    StringBuilder url =
        new StringBuilder(apiConnector.formatUrl(getResourceName(c)) + "/search?searchText=")
            .append(query.getSearchText());
    if (!query.getOrderBys().isEmpty()) {
      url.append("&orderBy=").append(
          query.getOrderBys().stream()
              .map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
              .collect(Collectors.joining(",")));
    }
    if (query.getPageSize() != null) {
      url.append("&pageSize=").append(query.getPageSize());
    }
    if (query.getPageToken() > 0) {
      url.append("pageToken=").append(query.getPageToken());
    }

    String json = apiConnector.sendGet(url.toString());
    JSONObject response = new JSONObject(json);

    int totalSize = response.getInt("totalSize");
    if (totalSize == 0) {
      return QueryResult.of(ImmutableList.of(), totalSize, query);
    }

    JSONArray resultArray = response.getJSONArray("items");
    ImmutableList<T> results = IntStream.range(0, resultArray.length())
        .mapToObj(x -> convert(c, resultArray.getJSONObject(x).toString()))
        .collect(toImmutableList());

    return QueryResult.of(results, totalSize, query);
  }

  @Override
  public <T, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
    return Calculate.executeWithRetries(2, () -> {
      T orig = get(c, id);
      JSONObject origJson = new JSONObject(orig);
      T updated = updater.apply(orig);
      JSONObject updatedJson = new JSONObject(updated);

      // Find all the fields that were modified in the updated proto.
      String updateMask = ImmutableSet.<String>builder()
          .addAll(origJson.keySet())
          .addAll(updatedJson.keySet())
          .build()
          .stream()
          .filter(field -> !Objects.equals(origJson.get(field), updatedJson.get(field)))
          .collect(Collectors.joining(","));

      if (updateMask.isEmpty()) {
        System.out.println("Nothing changed, returning");
      } else {
        String url = apiConnector.formatUrl(getResourceName(c)) + "/update?id=" + id + "&updateMask=" + updateMask;
        return convert(c, apiConnector.send("PUT", url, new JSONObject(updated).toString()));
      }
      return updated;
    });
  }

  @Override
  public <T, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
    throw new DD4StorageException("Unimplemented");
  }

  @Override
  public <T, I> boolean delete(Class<T> c, I id) {
    String url = apiConnector.formatUrl(getResourceName(c)) + "/delete?id=" + id;
    return Boolean.parseBoolean(apiConnector.send("DELETE", url, null));
  }

  @Override
  public <T, I> int delete(Class<T> c, Iterable<I> ids) {
    String url = apiConnector.formatUrl(getResourceName(c)) + "/batchDelete";
    return Integer.parseInt(
        apiConnector.send("POST", url, new JSONObject().put("items", ids).toString()).trim());
  }

  private <T> T convert(Class<T> cls, String content) {
    return JSONUtil.toObject(cls, content);
  }

  private <T> ImmutableList<T> convertList(Class<T> c, String content) {
    JSONArray array = new JSONArray(content);
    return IntStream.of(array.length())
        .mapToObj(i -> convert(c, array.getString(i)))
        .collect(toImmutableList());
  }

  private static String getResourceName(Class<?> cls) {
    return cls.getSimpleName().substring(0, 1).toLowerCase() + cls.getSimpleName().substring(1) + "s";
  }
}
