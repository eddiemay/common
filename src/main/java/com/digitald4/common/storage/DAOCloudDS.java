package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.SortDirection.ASCENDING;
import static com.google.appengine.api.datastore.Query.SortDirection.DESCENDING;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.JSONUtil;
import com.digitald4.common.util.JSONUtil.Field;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class DAOCloudDS implements DAO {
	private final DatastoreService datastoreService;
	private final ChangeTracker changeTracker;
	private final SearchIndexer searchIndexer;

	@Inject
	public DAOCloudDS(DatastoreService datastoreService,
			ChangeTracker changeTracker, SearchIndexer searchIndexer) {
		this.datastoreService = datastoreService;
		this.changeTracker = changeTracker;
		this.searchIndexer = searchIndexer;
	}

	@Override
	public <T> T create(T t) {
		return create(ImmutableList.of(t)).get(0);
	}

	@Override
	public <T> ImmutableList<T> create(Iterable<T> ts) {
		ImmutableList<T> items = ImmutableList.copyOf(ts);
		if (items.isEmpty()) {
			return items;
		}

		ImmutableMap<String, Field> fields = JSONUtil.getFields(ts.iterator().next().getClass());
		return Calculate.executeWithRetries(2, () -> {
			changeTracker.prePersist(ts);
			List<Key> keys = datastoreService.put(
					items.stream().map(item -> {
						JSONObject json = new JSONObject(item);
						Object id = json.opt("id");
						Entity entity = createEntity(item.getClass().getSimpleName(), id);
						json.keySet().forEach(fieldName -> setObject(entity, json, fieldName, fields));
						return entity;
					}).collect(toImmutableList()));
			for (int k = 0; k < keys.size(); k++) {
				Key key = keys.get(k);
				if (key.getId() > 0) {
					fields.get("id").invokeSet(items.get(k), key.getId());
				}
			}
			return changeTracker.postPersist(items, true);
		});
	}

	@Override
	public <T, I> T get(Class<T> c, I id) {
		return Calculate.executeWithRetries(2, () -> get(c, createFactorKey(c.getSimpleName(), id)));
	}

	@Override
	public <T, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
		return Calculate.executeWithRetries(2, () -> getEntities(c,ids).values().stream()
				.map(v -> convert(c, v))
				.collect(toImmutableList()));
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query.List query) {
		return Calculate.executeWithRetries(2,
				() -> QueryResult.transform(listEntities(c, query), entity -> convert(c, entity)));
	}

	@Override
	public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
		return searchIndexer.search(c, searchQuery);
	}

	@Override
	public <T, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
		return update(c, ImmutableList.of(id), updater).get(0);
	}

	@Override
	public <T, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
		String kind = c.getSimpleName();
		ImmutableMap<String, Field> fields = JSONUtil.getFields(c);
		return Calculate.executeWithRetries(2, () -> {
				ImmutableList<T> entities = getEntities(c, ids).values().stream()
						.map(entity -> updater.apply(convert(c, entity)))
						.collect(toImmutableList());
				changeTracker.prePersist(entities);
				datastoreService.put(entities.stream()
						.map(t -> {
							JSONObject json = new JSONObject(t);
							Object id = json.get("id");
							Entity entity = createEntity(kind, id);
							json.keySet().forEach(fieldName -> setObject(entity, json, fieldName, fields));
							return entity;
						})
						.collect(toImmutableList()));
				return changeTracker.postPersist(entities, false);
		});
	}

	@Override
	public <T, I> boolean delete(Class<T> c, I id) {
		return delete(c, ImmutableList.of(id)) == 1;
	}

	@Override
	public <T, I> int delete(Class<T> c, Iterable<I> ids) {
		return Calculate.executeWithRetries(2, () -> {
			changeTracker.preDelete(c, ids);
			datastoreService.delete(createFactorKeys(c, ids));
			changeTracker.postDelete(c, ids);
			return Iterables.size(ids);
		});
	}

	private static Entity createEntity(String kind, Object id) {
		if (id instanceof Long && (Long) id > 0L) {
			return new Entity(kind, (Long) id);
		} else if (id instanceof String) {
			return new Entity(kind, (String) id);
		}
		return new Entity(kind);
	}

	private <T> T get(Class<T> c, Key key) {
		try {
			return convert(c, datastoreService.get(key));
		} catch (EntityNotFoundException e) {
			throw new DD4StorageException(
					"Error fetching item: " + c.getSimpleName() + ":" + key.getId(), e, ErrorCode.NOT_FOUND);
		}
	}

	private static Key createFactorKey(String kind, Object id) {
		return (id instanceof Long) ? createKey(kind, (Long) id) : createKey(kind, id.toString());
	}

	private static <T,I> ImmutableList<Key> createFactorKeys(Class<T> c, Iterable<I> ids) {
		String kind = c.getSimpleName();
		return stream(ids).map(id -> createFactorKey(kind, id)).collect(toImmutableList());
	}

	private <T, I> ImmutableMap<Key, Entity> getEntities(Class<T> c, Iterable<I> ids) {
		Map<Key, Entity> results = datastoreService.get(createFactorKeys(c, ids));
		if (results.size() != Iterables.size(ids)) {
			throw new DD4StorageException(
					String.format("One or more items not found while fetching: %s. Requested: %d, found: %d",
							c.getSimpleName(), Iterables.size(ids), results.size()), ErrorCode.NOT_FOUND);
		}

		return ImmutableMap.copyOf(results);
	}

	private QueryResult<Entity> listEntities(Class<?> c, Query.List query) {
		com.google.appengine.api.datastore.Query eQuery =
				new com.google.appengine.api.datastore.Query(c.getSimpleName());
		if (!query.getFilters().isEmpty()) {
			if (query.getFilters().size() == 1) {
				eQuery.setFilter(convertToPropertyFilter(c, query.getFilters().get(0)));
			} else {
				eQuery.setFilter(
						new CompositeFilter(CompositeFilterOperator.AND, query.getFilters().stream()
								.map(filter -> convertToPropertyFilter(c, filter))
								.collect(toImmutableList())));
			}
		}
		query.getOrderBys().forEach(
				orderBy -> eQuery.addSort(orderBy.getColumn(), orderBy.getDesc() ? DESCENDING : ASCENDING));

		Iterable<Entity> allResults = datastoreService.prepare(eQuery).asIterable();

		Integer limit = query.getLimit();

		return QueryResult.of(
				Streams.stream(allResults)
						.skip(query.getOffset())
						.limit(limit == null || limit == 0 ? Integer.MAX_VALUE : limit)
						.collect(toImmutableList()),
				Iterables.size(allResults), query);
	}

	private void setObject(
			Entity entity, JSONObject json, String fieldName, ImmutableMap<String, Field> fields) {
		if (fieldName.equals("id")) {
			return;
		}

		Field field = fields.get(fieldName);
		Object value = json.get(fieldName);
		if (value instanceof JSONArray && field.getType().getSimpleName().equals("byte[]")) {
			JSONArray jsonArray = json.getJSONArray(fieldName);
			byte[] bytes = new byte[jsonArray.length()];
			for (int b = 0; b < bytes.length; b++) {
				bytes[b] = (Byte) jsonArray.get(b);
			}
			setProperty(entity, fieldName, new Blob(bytes), field);
		} else if (value instanceof JSONObject || value instanceof JSONArray) {
			setProperty(entity, fieldName, new Text(value.toString()), field);
		} else if (value instanceof Enum) {
			setProperty(entity, fieldName, value.toString(), field);
		} else if (value instanceof DateTime) {
			setProperty(entity, fieldName, new Date(((DateTime) value).getMillis()), field);
		} else if (value instanceof Instant) {
			setProperty(entity, fieldName, new Date(((Instant) value).toEpochMilli()), field);
		} else if (value instanceof Long && (fields.get(fieldName).getType() == DateTime.class
				|| fields.get(fieldName).getType() == Instant.class)) {
			setProperty(entity, fieldName, new Date((Long) value), field);
		} else if (value instanceof StringBuilder) {
			setProperty(entity, fieldName, new Text(value.toString()), field);
		} else {
			setProperty(entity, fieldName, value, field);
		}
	}

	private static void setProperty(Entity entity, String fieldName, Object value, Field field) {
		if (field.isNonIndexed()) {
			entity.setUnindexedProperty(fieldName, value);
		} else {
			entity.setProperty(fieldName, value);
		}
	}

	private <T> T convert(Class<T> c, Entity entity) {
		if (entity == null) {
			return null;
		}

		ImmutableMap<String, Field> fieldMap = JSONUtil.getFields(c);
		JSONObject jsonObject = new JSONObject();
		Field idField = fieldMap.get("id");
		if (idField != null && idField.getSetMethod() != null) {
			Class<?> idType = idField.getSetMethod().getParameterTypes()[0];
			jsonObject.put(
					"id", idType == String.class ? entity.getKey().getName() : entity.getKey().getId());
		}
		entity.getProperties().forEach((colName, value) -> {
			String javaName = FormatText.toLowerCamel(colName);
			Field field = fieldMap.get(javaName);
			if (field == null) {
				throw new DD4StorageException(
						"Unknown field: " + javaName + " for Object: " + c.getSimpleName());
			}

			if (field.getSetMethod() == null) {
				return;
			}

			if (value instanceof Text) {
				value = ((Text) value).getValue();
			}

			switch (field.getType().getSimpleName()) {
				case "Boolean":
				case "boolean":
					jsonObject.put(javaName, ((Boolean) value).booleanValue());
					break;
				case "ByteArray":
					jsonObject.put(javaName, value.toString().getBytes());
					break;
				case "byte[]":
				case "Byte[]":
					jsonObject.put(javaName, ((Blob) value).getBytes());
					break;
				case "DateTime":
					if (value instanceof Date) {
						jsonObject.put(javaName, ((Date) value).getTime());
					} else {
						jsonObject.put(javaName,
								(value instanceof Long) ? value : DateTime.parse((String) value).getMillis());
					}
					break;
				case "Instant":
					if (value instanceof Date) {
						jsonObject.put(javaName, ((Date) value).getTime());
					} else {
						jsonObject.put(javaName,
								(value instanceof Long) ? value : Instant.parse((String) value).toEpochMilli());
					}
					break;
				case "Integer":
				case "int":
					jsonObject.put(javaName, ((Long) value).intValue());
					break;
				case "Long":
				case "long":
					if (colName.endsWith("id")) {
						jsonObject.put(javaName, value);
					} else {
						jsonObject.put(javaName, value);
						// field.invokeSet(t, new java.sql.Timestamp((Long.parseLong(value.toString()))));
					}
					break;
				case "StringBuilder":
					jsonObject.put(javaName, new StringBuilder((String) value));
					break;
				case "String":
				default:
					if (field.isCollection()) {
						jsonObject.put(javaName, new JSONArray((String) value));
					} else if (field.getType().isEnum()) {
						jsonObject.put(javaName,
								Enum.valueOf((Class<? extends Enum>) field.getType(), (String) value));
					} else {
						jsonObject.put(javaName, field.isObject() ? new JSONObject((String) value) : value);
					}
					break;
			}
		});

		return JSONUtil.toObject(c, jsonObject);
	}

	private FilterPredicate convertToPropertyFilter(Class<?> c, Query.Filter filter) {
		String fieldName = FormatText.toLowerCamel(filter.getColumn());
		Field field = JSONUtil.getFields(c).get(fieldName);
		if (field == null) {
			throw new DD4StorageException("Unknown column: " + fieldName);
		}

		Object value = filter.getValue();
		if (value != null) {
			switch (field.getType().getSimpleName()) {
				case "Long":
				case "long":
					value = value instanceof Collection
							? ((Collection<?>) value).stream()
							.map(Object::toString)
							.map(Long::parseLong)
							.collect(toImmutableList())
							: Long.parseLong(value.toString());
					break;
				case "Integer":
				case "int":
					value = value instanceof Collection
							? ((Collection<?>) value).stream()
							.map(Object::toString)
							.map(Integer::parseInt)
							.collect(toImmutableList())
							: Integer.parseInt(value.toString());
					break;
				case "Boolean":
				case "boolean":
					value = Boolean.parseBoolean(value.toString());
					break;
				case "Double":
				case "double":
					value = Double.parseDouble(value.toString());
					break;
				case "Float":
				case "float":
					value = Float.parseFloat(value.toString());
					break;
				case "DateTime":
					value = Long.parseLong(value.toString()) * 1000;
					break;
				case "Instant":
					value = new Date(Long.parseLong(value.toString()));
				default:
					value = value instanceof Collection ?
							((Collection<?>) value).stream().map(Object::toString).collect(toImmutableList())
							: value;
			}
		}

		switch (filter.getOperator()) {
			case "<":
				return new FilterPredicate(fieldName, FilterOperator.LESS_THAN, value);
			case "<=":
				return new FilterPredicate(fieldName, FilterOperator.LESS_THAN_OR_EQUAL, value);
			case "=":
			case "":
				return new FilterPredicate(fieldName, FilterOperator.EQUAL, value);
			case ">=":
				return new FilterPredicate(fieldName, FilterOperator.GREATER_THAN_OR_EQUAL, value);
			case ">":
				return new FilterPredicate(fieldName, FilterOperator.GREATER_THAN, value);
			case "IN":
				return new FilterPredicate(fieldName, FilterOperator.IN, value);
			default:
				throw new IllegalArgumentException("Unknown operator " + filter.getOperator());
		}
	}
}
