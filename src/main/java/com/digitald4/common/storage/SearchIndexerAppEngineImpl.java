package com.digitald4.common.storage;

import static com.google.appengine.api.search.SortExpression.SortDirection.ASCENDING;
import static com.google.appengine.api.search.SortExpression.SortDirection.DESCENDING;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.JSONUtil;
import com.digitald4.common.util.JSONUtil.Field;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.search.SortExpression;
import com.google.appengine.api.search.SortOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class SearchIndexerAppEngineImpl implements SearchIndexer {
  private static final int UPDATE_LIMIT = 200;
  private final Map<Class<?>, Index> indexes = new HashMap<>();

  @Override
  public <T extends Searchable> void index(Iterable<T> entities) {
    if (Iterables.isEmpty(entities)) {
      return;
    }

    Index index = getIndex(entities.iterator().next().getClass());
    for (int x = 0; x < Iterables.size(entities); x += UPDATE_LIMIT) {
      index.putAsync(stream(entities).skip(x).limit(UPDATE_LIMIT).map(this::toDocument)
          .collect(toImmutableList()));
    }
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
    SortOptions.Builder sortOptions = SortOptions.newBuilder();
    searchQuery.getOrderBys().forEach(
        orderBy -> sortOptions.addSortExpression(
            SortExpression.newBuilder()
                .setExpression(orderBy.getColumn())
                .setDirection(orderBy.getDesc() ? DESCENDING : ASCENDING)));

    Results<ScoredDocument> results = getIndex(c).search(
        com.google.appengine.api.search.Query.newBuilder()
            .setOptions(
                QueryOptions.newBuilder()
                    .setSortOptions(sortOptions)
                    .setLimit(searchQuery.getLimit())
                    .setOffset(searchQuery.getOffset()))
            .build(searchQuery.getSearchText()));

    ImmutableList<T> ts = results.getResults().stream()
        .map(document -> fromDocument(c, document))
        .collect(toImmutableList());

    return QueryResult.of(ts, (int) Math.min(results.getNumberFound(), 1000), searchQuery);
  }

  @Override
  public <T extends Searchable> void removeIndex(Class<T> c, Iterable<?> ids) {
    Index index = getIndex(c);
    for (int x = 0; x < Iterables.size(ids); x += UPDATE_LIMIT) {
      index.delete(stream(ids)
          .skip(x).limit(UPDATE_LIMIT).map(Object::toString).collect(toImmutableList()));
    }
  }

  private Index getIndex(Class<?> c) {
    return indexes.computeIfAbsent(c, this::computeIndex);
  }

  protected Index computeIndex(Class<?> c) {
    return SearchServiceFactory.getSearchService()
        .getIndex(IndexSpec.newBuilder().setName(c.getSimpleName()).build());
  }

  protected <T> Document toDocument(T t) {
    Document.Builder document = Document.newBuilder();
    JSONObject json = new JSONObject(t);
    ImmutableMap<String, Field> fields = JSONUtil.getFields(t.getClass());
    json.keySet().forEach(fieldName -> {
      Field field = fields.get(fieldName);
      if (field == null) {
        return;
      }

      if (fieldName.equals("id")) {
        document.setId(json.get(fieldName).toString());
        return;
      }

      com.google.appengine.api.search.Field.Builder docField =
          com.google.appengine.api.search.Field.newBuilder().setName(fieldName);

      switch (field.getType().getSimpleName()) {
        case "ByteArray":
        case "byte[]":
        case "Byte[]":
          docField.setText(json.getString(fieldName));
          break;
        case "DateTime":
        case "Instant":
          docField.setDate(new Date(json.getLong(fieldName)));
          break;
        case "Boolean":
        case "boolean":
          docField.setAtom(String.valueOf(json.getBoolean(fieldName)));
        case "Integer":
        case "int":
        case "Float":
        case "float":
        case "Long":
        case "long":
        case "Double":
        case "double":
          docField.setNumber(json.getDouble(fieldName));
          break;
        case "StringBuilder":
          docField.setText(json.get(fieldName).toString());
          break;
        case "String":
          docField.setText(json.getString(fieldName));
          break;
        default:
          if (field.isCollection()) {
            docField.setText(json.get(fieldName).toString());
          } else if (field.getType().isEnum()) {
            docField.setAtom(json.get(fieldName).toString());
          } else if (field.isObject()) {
            docField.setText(json.get(fieldName).toString());
          } else {
            throw new DD4StorageException("Unhandled search field type: " + field);
          }
          break;
      }
      document.addField(docField);
    });
    return document.build();
  }

  @VisibleForTesting
  protected <T> T fromDocument(Class<T> c, Document document) {
    if (document == null) {
      return null;
    }

    ImmutableMap<String, Field> fieldMap = JSONUtil.getFields(c);
    JSONObject json = new JSONObject();
    Field idField = fieldMap.get("id");
    if (idField != null && idField.getSetMethod() != null) {
      json.put("id", document.getId());
    }
    document.getFields().forEach(docField -> {
      String javaName = docField.getName();
      Field field = fieldMap.get(javaName);
      if (field == null) {
        return;
      }

      if (field.getSetMethod() == null) {
        return;
      }

      switch (field.getType().getSimpleName()) {
        case "ByteArray":
        case "byte[]":
        case "Byte[]":
          json.put(javaName, docField.getText().getBytes());
          break;
        case "DateTime":
          json.put(javaName, docField.getDate().getTime());
          break;
        case "Instant":
          json.put(javaName, docField.getDate().getTime());
          break;
        case "Boolean":
        case "boolean":
          json.put(javaName, Boolean.valueOf(docField.getText()));
        case "Integer":
        case "int":
          json.put(javaName, docField.getNumber().intValue());
          break;
        case "Long":
        case "long":
          json.put(javaName, docField.getNumber().longValue());
          break;
        case "StringBuilder":
          json.put(javaName,
              Strings.isNullOrEmpty(docField.getText()) ? docField.getHTML() : docField.getText());
          break;
        case "String":
          json.put(javaName,
              Strings.isNullOrEmpty(docField.getText()) ? docField.getAtom() : docField.getText());
          break;
        default:
          if (field.isCollection()) {
            json.put(javaName, new JSONArray(docField.getText()));
          } else if (field.getType().isEnum()) {
            json.put(javaName,
                Enum.valueOf((Class<? extends Enum>) field.getType(), docField.getAtom()));
          } else if (field.isObject()) {
            json.put(javaName, new JSONObject(docField.getText()));
          } else {
            throw new DD4StorageException("Unhandled search field type: " + field);
          }
          break;
      }
    });

    return JSONUtil.toObject(c, json);
  }
}
