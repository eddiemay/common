package com.digitald4.common.storage;

import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.SortDirection.ASCENDING;
import static com.google.appengine.api.datastore.Query.SortDirection.DESCENDING;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.storage.Transaction.Op;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Provider;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class DAOAppEngineDatastore implements DAO {
	private final Provider<Context> contextProvider;
	private final DatastoreService datastoreService;
	private final ChangeTracker changeTracker;
	private final SearchIndexer searchIndexer;

	@Inject
	public DAOAppEngineDatastore(
			Provider<Context> contextProvider, ChangeTracker changeTracker, SearchIndexer searchIndexer) {
		this.contextProvider = contextProvider;
		this.changeTracker = changeTracker;
		this.searchIndexer = searchIndexer;
		this.datastoreService = DatastoreServiceFactory.getDatastoreService();
	}

	@Override
	public <T> Transaction<T> persist(Transaction<T> transaction) {
		ImmutableList<Op<T>> ops = transaction.getOps();
		if (ops.isEmpty()) {
			return transaction;
		}

		Class<T> c = ops.get(0).getTypeClass();
		String kind = getTableName(c);
		ImmutableMap<String, Field> fields = JSONUtil.getFields(c);
		return Calculate.executeWithRetries(2, () -> {
			changeTracker.prePersist(this, transaction);
			var keys = datastoreService.put(ops.stream()
					.map(op -> {
						JSONObject json = new JSONObject(op.getEntity());
						Object id = op.getId();
						Entity entity = createEntity(kind, id);
						json.keySet().forEach(fieldName -> setObject(entity, json, fieldName, fields));
						return entity;
					})
					.collect(toImmutableList()));
			for (int k = 0; k < keys.size(); k++) {
				Key key = keys.get(k);
				if (key.getId() > 0) {
					ops.get(k).setId(key.getId());
					fields.get("id").invokeSet(ops.get(k).getEntity(), key.getId());
				}
			}
			changeTracker.postPersist(this, transaction);
			return transaction;
		});
	}

	@Override
	public <T, I> T get(Class<T> c, I id) {
		return Calculate.executeWithRetries(2, () -> get(c, createFactorKey(getTableName(c), id)));
	}

	@Override
	public <T, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids) {
		return Calculate.executeWithRetries(2, () -> getEntities(c, ids));
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query.List query) {
		return Calculate.executeWithRetries(2, () -> listEntities(c, query));
	}

	@Override
	public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
		return searchIndexer.search(c, searchQuery);
	}

	@Override
	public <T, I> boolean delete(Class<T> c, I id) {
		return delete(c, ImmutableList.of(id)) == 1;
	}

	@Override
	public <T, I> int delete(Class<T> c, Iterable<I> ids) {
    try {
			Class<?> rt = c.getMethod("getId").getReturnType();
			ImmutableList<?> idList = (rt == Long.class || rt.getGenericSuperclass() == Long.class)
					? stream(ids).map(String::valueOf).map(Long::parseLong).collect(toImmutableList())
					: ImmutableList.copyOf(ids);

			return Calculate.executeWithRetries(2, () -> {
				changeTracker.preDelete(this, c, idList);
				datastoreService.delete(createFactorKeys(c, idList));
				changeTracker.postDelete(c, idList);
				return idList.size();
			});
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
	}

	@Override
	public <T, I> int index(Class<T> c, Iterable<T> items) {
		if (Iterables.isEmpty(items)) {
			return 0;
		}

		T first = items.iterator().next();
		if (!(first instanceof Searchable)) {
			throw new IllegalArgumentException(
					String.format("Can not index, %s not instance of Searchable", c));
		}

		return searchIndexer.index((Iterable<? extends Searchable>) items);
	}

	private String getTableName(Class<?> c) {
		Context context = contextProvider.get();
		return (context == Context.NONE) ? c.getSimpleName() : String.format("%s.%s", context.name(), c.getSimpleName());
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
			return convert(c, datastoreService.get(key), ImmutableSet.of());
		} catch (EntityNotFoundException e) {
			throw new DD4StorageException(
					"Error fetching: " + getTableName(c) + ":" + (key.getId() > 0 ? key.getId() : key.getName()),
					e, ErrorCode.NOT_FOUND);
		}
	}

	private static Key createFactorKey(String kind, Object id) {
		return (id instanceof Long) ? createKey(kind, (Long) id) : createKey(kind, id.toString());
	}

	private <T,I> ImmutableList<Key> createFactorKeys(Class<T> c, Iterable<I> ids) {
		String kind = getTableName(c);
		return stream(ids).map(id -> createFactorKey(kind, id)).collect(toImmutableList());
	}

	private <T, I> BulkGetable.MultiListResult<T, I> getEntities(Class<T> c, Iterable<I> ids) {
		return BulkGetable.MultiListResult.of(
				datastoreService.get(createFactorKeys(c, ids)).values().stream()
						.map(entity -> convert(c, entity, ImmutableSet.of())).collect(toImmutableList()), ids);
	}

	private <T> QueryResult<T> listEntities(Class<T> c, Query.List query) {
		com.google.appengine.api.datastore.Query eQuery = new com.google.appengine.api.datastore.Query(getTableName(c));
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

		Integer limit = query.getLimit();
		if (limit == null || limit == 0) {
			limit = Integer.MAX_VALUE;
		}

		if (query.useDBSort()) {
			query.getOrderBys().forEach(orderBy -> eQuery.addSort(orderBy.getColumn(), orderBy.getDesc() ? DESCENDING : ASCENDING));
			var results = datastoreService.prepare(eQuery).asList(FetchOptions.Builder.withLimit(limit).offset(query.getOffset()));
			return QueryResult.of(
					c, results.stream().map(entity -> convert(c, entity, query.getFields())).collect(toImmutableList()), results.size(), query);
		}

		var allResults = datastoreService.prepare(eQuery).asList(FetchOptions.Builder.withLimit(2750));

		var comparator = getComparator(c, query);
		return QueryResult.of(c,
				allResults.stream()
						.map(entity -> convert(c, entity, query.getFields()))
						.sorted(comparator)
						.skip(query.getOffset())
						.limit(limit)
						.collect(toImmutableList()),
				allResults.size(), query);
	}

	private void setObject(Entity entity, JSONObject json, String fieldName, ImmutableMap<String, Field> fields) {
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
		} else if (value instanceof JSONObject || value instanceof JSONArray || value instanceof StringBuilder) {
			setProperty(entity, fieldName, new Text(value.toString()), field);
		} else if (value instanceof Enum) {
			setProperty(entity, fieldName, value.toString(), field);
		} else if (value instanceof DateTime dateTime) {
			setProperty(entity, fieldName, dateTime.getMillis(), field);
		} else if (value instanceof Instant instant) {
			setProperty(entity, fieldName, instant.toEpochMilli(), field);
		} else if (value instanceof Long && (fields.get(fieldName).getType() == DateTime.class
				|| fields.get(fieldName).getType() == Instant.class)) {
			setProperty(entity, fieldName, new Date((Long) value), field);
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

	private <T> T convert(Class<T> c, Entity entity, Iterable<String> selectdFields) {
		if (entity == null) {
			return null;
		}

		ImmutableSet<String> fieldSet = ImmutableSet.copyOf(selectdFields);

		ImmutableMap<String, Field> fieldMap = JSONUtil.getFields(c);
		JSONObject jsonObject = new JSONObject();
		Field idField = fieldMap.get("id");
		if (idField != null && idField.getSetMethod() != null) {
			Key key = entity.getKey();
			jsonObject.put("id", key.getId() > 0 ? key.getId() : key.getName());
		}
		entity.getProperties().forEach((colName, value) -> {
			String javaName = FormatText.toLowerCamel(colName);
			Field field = fieldMap.get(javaName);
			if (field == null) {
				throw new DD4StorageException("Unknown field: " + javaName + " for Object: " + c.getSimpleName());
			}

			if (!fieldSet.isEmpty() && !fieldSet.contains(colName)) {
				return;
			}

			if (field.getSetMethod() == null) {
				return;
			}

			if (value instanceof Text) {
				value = ((Text) value).getValue();
			}

			try {

				switch (field.getType().getSimpleName()) {
					case "Boolean", "boolean" -> jsonObject.put(javaName, ((Boolean) value).booleanValue());
					case "ByteArray" -> jsonObject.put(javaName, value.toString().getBytes());
					case "byte[]", "Byte[]" -> jsonObject.put(javaName, ((Blob) value).getBytes());
					case "DateTime" -> {
						if (value instanceof Date) {
							jsonObject.put(javaName, ((Date) value).getTime());
						} else {
							jsonObject.put(javaName,
									(value instanceof Long) ? value : DateTime.parse((String) value).getMillis());
						}
					}
					case "Double", "double" -> jsonObject.put(javaName, ((Double) value).doubleValue());
					case "Instant" -> {
						if (value instanceof Date) {
							jsonObject.put(javaName, ((Date) value).getTime());
						} else {
							jsonObject.put(javaName,
									(value instanceof Long) ? value : Instant.parse((String) value).toEpochMilli());
						}
					}
					case "Integer", "int" -> jsonObject.put(javaName, ((Long) value).intValue());
					case "Long", "long" -> {
						if (colName.endsWith("id")) {
							jsonObject.put(javaName, value);
						} else {
							jsonObject.put(javaName, value);
							// field.invokeSet(t, new java.sql.Timestamp((Long.parseLong(value.toString()))));
						}
					}
					case "StringBuilder" -> jsonObject.put(javaName, new StringBuilder((String) value));
					default -> {
						if (field.isCollection()) {
							jsonObject.put(javaName, new JSONArray((String) value));
						} else if (field.getType().isEnum()) {
							jsonObject.put(javaName, Enum.valueOf((Class<? extends Enum>) field.getType(), (String) value));
						} else {
							jsonObject.put(javaName, field.isObject() ? new JSONObject((String) value) : value);
						}
					}
				}
			} catch (ClassCastException cce) {
				throw new DD4StorageException("Error reading column: " + colName, cce);
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
					break;
				default:
					value = value instanceof Collection ?
							((Collection<?>) value).stream().map(Object::toString).collect(toImmutableList()) : String.valueOf(value);
			}
		}

		return switch (filter.getOperator()) {
			case "<" -> new FilterPredicate(fieldName, FilterOperator.LESS_THAN, value);
			case "<=" -> new FilterPredicate(fieldName, FilterOperator.LESS_THAN_OR_EQUAL, value);
			case "=", "" -> new FilterPredicate(fieldName, FilterOperator.EQUAL, value);
			case "!=" -> new FilterPredicate(fieldName, FilterOperator.NOT_EQUAL, value);
			case ">=" -> new FilterPredicate(fieldName, FilterOperator.GREATER_THAN_OR_EQUAL, value);
			case ">" -> new FilterPredicate(fieldName, FilterOperator.GREATER_THAN, value);
			case "IN" -> new FilterPredicate(fieldName, FilterOperator.IN, value);
			default -> throw new IllegalArgumentException("Unknown operator " + filter.getOperator());
		};
	}

	private static <T> Comparator<T> getComparator(Class<T> c, Query query) {
		if (query.getOrderBys().isEmpty()) {
			 return (o1, o2) -> 0;
		}

		Comparator<T> comparator = getComparator(c, query.getOrderBys().get(0));
		for (int ob = 1; ob < query.getOrderBys().size(); ob++) {
			comparator = comparator.thenComparing(getComparator(c, query.getOrderBys().get(ob)));
		}

		return comparator;
	}

	private static <T> Comparator<T> getComparator(Class<T> c, Query.OrderBy orderBy) {
		Method method = getMethod(c, orderBy);
		int multiplier = orderBy.getDesc() ? -1 : 1;
		return (c1, c2) -> {
			try {
				Object o1 = method.invoke(c1);
				Object o2 = method.invoke(c2);
				if (o1 == null && o2 == null) {
					return 0;
				} else if (o1 == null) {
					return -1 * multiplier;
				} else if (o2 == null) {
					return multiplier;
				} else if (o1 instanceof Integer integer) {
					return integer.compareTo((Integer) o2) * multiplier;
				} else if (o1 instanceof Double d) {
					return d.compareTo((Double) o2) * multiplier;
				}
				return o1.toString().compareTo(o2.toString()) * multiplier;
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		};
	}

	private static <T> Method getMethod(Class<T> c, Query.OrderBy orderBy) {
		String columnName = orderBy.getColumn();
		try {
			return c.getMethod("get" + columnName.substring(0, 1).toUpperCase() + columnName.substring(1));
		} catch (NoSuchMethodException e) {
			// Fall out and try the name without the get.
		}
		try {
			return c.getMethod(columnName);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
}
