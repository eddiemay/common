package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

public class DAOApiImpl implements DAO {
  private final ObjectMapper mapper = new ObjectMapper();
  private final APIConnector apiConnector;

  public DAOApiImpl(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public <T> T create(T t) {
    String url = apiConnector.formatUrl(getResourceName(t.getClass())) + "/_";
    return convert((Class<T>) t.getClass(), apiConnector.sendPost(url, new JSONObject(t).toString()));
  }

  @Override
  public <T> T get(Class<T> c, long id) {
    String url = apiConnector.formatUrl(getResourceName(c)) + "/" + id;
    return convert(c, apiConnector.sendGet(url));
  }

  @Override
  public <T> QueryResult<T> list(Class<T> c, Query query) {
    StringBuilder url = new StringBuilder(apiConnector.formatUrl(getResourceName(c)) + "/_?");

    url.append("filter=").append(query.getFilters().stream()
        .map(filter -> filter.getColumn() + filter.getOperator() + filter.getValue())
        .collect(Collectors.joining(",")));
    if (!query.getOrderBys().isEmpty()) {
      url.append("&orderBy=").append(query.getOrderBys().stream()
          .map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
          .collect(Collectors.joining(",")));
    }
    if (query.getLimit() > 0) {
      url.append("&pageSize").append("=").append(query.getLimit());
    }
    if (query.getOffset() > 0) {
      url.append("&pageToken").append("=").append(query.getOffset());
    }

    String json = apiConnector.sendGet(url.toString());
    System.out.println("json result: " + json);
    JSONObject response = new JSONObject(json);

    int totalSize = response.getInt("totalSize");
    List<T> results = new ArrayList<>(totalSize);
    if (totalSize > 0) {
      JSONArray resultArray = response.getJSONArray("results");
      for (int x = 0; x < resultArray.length(); x++) {
        results.add(convert(c, resultArray.getJSONObject(x).toString()));
      }
    }

    return new QueryResult<>(results, response.getInt("totalSize"));
  }

  @Override
  public <T> T update(Class<T> c, long id, UnaryOperator<T> updater) {
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
          .map(FormatText::toUnderScoreCase)
          .collect(Collectors.joining(","));

      if (updateMask.isEmpty()) {
        System.out.println("Nothing changed, returning");
      } else {
        String url = apiConnector.formatUrl(getResourceName(c)) + "/" + id + "?updateMask=" + updateMask;
        return convert(c, apiConnector.send("PUT", url, new JSONObject(updated).toString()));
      }
      return updated;
    });
  }

  @Override
  public <T> void delete(Class<T> c, long id) {
    String url = apiConnector.formatUrl(getResourceName(c)) + "/" + id;
    apiConnector.send("DELETE", url, null);
  }

  @Override
  public <T> int delete(Class<T> c, Query query) {
    StringBuilder url = new StringBuilder(apiConnector.formatUrl(getResourceName(c)) + ":batchDelete?");

    url.append("filter=").append(query.getFilters().stream()
        .map(filter -> filter.getColumn() + filter.getOperator() + filter.getValue())
        .collect(Collectors.joining(",")));
    if (!query.getOrderBys().isEmpty()) {
      url.append("&orderBy=").append(query.getOrderBys().stream()
          .map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
          .collect(Collectors.joining(",")));
    }
    if (query.getLimit() > 0) {
      url.append("&pageSize").append("=").append(query.getLimit());
    }
    if (query.getOffset() > 0) {
      url.append("&pageToken").append("=").append(query.getOffset());
    }

    return new JSONObject(apiConnector.send("DELETE", url.toString(), null)).getInt("deleted");
  }

  private <T> T convert(Class<T> c, String content) {
    try {
      return mapper.readValue(content, c);
    } catch (IOException e) {
      throw new DD4StorageException("Error converting object", e);
    }
  }

  private static String getResourceName(Class<?> cls) {
    return FormatText.toLowerCamel(cls.getSimpleName()) + "s";
  }
}
