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
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
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
		return Calculate.executeWithRetries(2, () ->
				changeTracker.postPersist(
						stream(changeTracker.prePersist(ts)).map(item -> {
							JSONObject json = new JSONObject(item);
							Object id = json.opt("id");
							Entity entity = createEntity(item.getClass().getSimpleName(), id);
							ImmutableMap<String, Field> fields = JSONUtil.getFields(item.getClass());
							json.keySet().forEach(fieldName -> setObject(entity, json, fieldName, fields));

							Key key = datastoreService.put(entity);
							return (key.getId() > 0) ? fields.get("id").invokeSet(item, key.getId()) : item;
						}).collect(toImmutableList()), true));
	}

	@Override
	public <T, I> T get(Class<T> c, I id) {
		return Calculate.executeWithRetries(2, () -> get(c, createFactorKey(c.getSimpleName(), id)));
	}

	@Override
	public <T, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
		final String name = c.getSimpleName();
		return Calculate.executeWithRetries(2, () ->
				getEntities(c, stream(ids).map(id -> createFactorKey(name, id)).collect(toImmutableList()))
						.values()
						.stream()
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
		String name = c.getSimpleName();
		return Calculate.executeWithRetries(2, () ->
				changeTracker.postPersist(
						stream(
								changeTracker.prePersist(
										getEntities(c, stream(ids).map(id -> createFactorKey(name, id)).collect(toImmutableList()))
												.values()
												.stream()
												.map(entity -> updater.apply(convert(c, entity)))
												.collect(toImmutableList())))
								.peek(t -> {
									JSONObject json = new JSONObject(t);
									ImmutableMap<String, Field> fields = JSONUtil.getFields(t.getClass());
									Object id = json.get("id");
									Entity entity =
											(id instanceof Long) ? new Entity(name, (Long) id) : new Entity(name, (String) id);
									json.keySet().forEach(fieldName -> setObject(entity, json, fieldName, fields));
									datastoreService.put(entity);
								})
								.collect(toImmutableList()), false));
	}

	@Override
	public <T, I> void delete(Class<T> c, I id) {
		delete(c, ImmutableList.of(id));
	}

	@Override
	public <T, I> void delete(Class<T> c, Iterable<I> ids) {
		Calculate.executeWithRetries(2, () -> {
			changeTracker.preDelete(c, ids);

			ImmutableList<Key> keys =
					stream(ids).map(id -> createFactorKey(c.getSimpleName(), id)).collect(toImmutableList());
			datastoreService.delete(keys);

			changeTracker.postDelete(c, ids);
			return keys.size();
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

	private <T> ImmutableMap<Key, Entity> getEntities(Class<T> c, Iterable<Key> keys) {
		ImmutableMap<Key, Entity> results = ImmutableMap.copyOf(datastoreService.get(keys));
		if (results.size() != Iterables.size(keys)) {
			throw new DD4StorageException("One of more items not found", ErrorCode.NOT_FOUND);
		}

		return results;
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
		/* Rather than use limit, loop over all items and add until limit to get a total count.
		if (request.getLimit() > 0) {
			query.setLimit(request.getLimit());
		}*/
		query.getOrderBys().forEach(
				orderBy -> eQuery.addSort(orderBy.getColumn(), orderBy.getDesc() ? DESCENDING : ASCENDING));

		ImmutableList.Builder<Entity> results = ImmutableList.builder();
		AtomicInteger count = new AtomicInteger();
		int end = query.getLimit() == null || query.getLimit() == 0
				? Integer.MAX_VALUE : query.getOffset() + query.getLimit();
		datastoreService.prepare(eQuery).asIterator().forEachRemaining(entity -> {
			if (count.getAndIncrement() >= query.getOffset() && count.get() <= end) {
				results.add(entity);
			}
		});

		return QueryResult.of(results.build(), count.get(), query);
	}

	private void setObject(
			Entity entity, JSONObject json, String fieldName, ImmutableMap<String, Field> fields) {
		if (fieldName.equals("id")) {
			return;
		}

		Object value = json.get(fieldName);
		if (value instanceof JSONArray
				&& fields.get(fieldName).getType().getSimpleName().equals("byte[]")) {
			JSONArray jsonArray = json.getJSONArray(fieldName);
			byte[] bytes = new byte[jsonArray.length()];
			for (int b = 0; b < bytes.length; b++) {
				bytes[b] = (Byte) jsonArray.get(b);
			}
			entity.setProperty(fieldName, new Blob(bytes));
		} else if (value instanceof JSONObject || value instanceof JSONArray) {
			entity.setProperty(fieldName, value.toString());
		} else if (value instanceof Enum) {
			entity.setProperty(fieldName, value.toString());
		} else if (value instanceof DateTime) {
			entity.setProperty(fieldName, new Date(((DateTime) value).getMillis()));
		} else if (value instanceof Instant) {
			entity.setProperty(fieldName, new Date(((Instant) value).toEpochMilli()));
		} else if (value instanceof Long && fields.get(fieldName).getType() == DateTime.class) {
			entity.setProperty(fieldName, new Date((Long) value));
		} else if (value instanceof StringBuilder) {
			entity.setProperty(fieldName, new Text(value.toString()));
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

			switch (field.getType().getSimpleName()) {
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
					jsonObject.put(javaName, (value instanceof Text) ? ((Text) value).getValue() : value);
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
			default:
				value = value instanceof Collection ?
						((Collection<?>) value).stream().map(Object::toString).collect(toImmutableList())
						: value;
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
