package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.JSONUtil;
import com.digitald4.common.util.JSONUtil.Field;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class DAOCloudDatastore implements DAO {
	public enum Context {PROD, TEST, NONE}
	private final Provider<Context> contextProvider;
	private final ChangeTracker changeTracker;
	private final SearchIndexer searchIndexer;
	private volatile Datastore datastore;

	@Inject
	public DAOCloudDatastore(
			Provider<Context> contextProvider, ChangeTracker changeTracker, SearchIndexer searchIndexer) {
		this.contextProvider = contextProvider;
		this.changeTracker = changeTracker;
		this.searchIndexer = searchIndexer;
	}

	@Override
	public <T> Transaction<T> persist(Transaction<T> transaction) {
		ImmutableList<Op<T>> ops = transaction.getOps();
		Class<T> c = ops.get(0).getTypeClass();
		String kind = getTableName(c);
		ImmutableMap<String, Field> fields = JSONUtil.getFields(c);
		return Calculate.executeWithRetries(2, () -> {
			changeTracker.prePersist(this, transaction);
			var gcpTransaction = getDatastore().newTransaction();
			ops.forEach(op -> {
				JSONObject json = new JSONObject(op.getEntity());
				Object id = op.getId();
				var entity = createEntity(kind, id);
				json.keySet().forEach(fieldName -> setObject(entity, json, fieldName, fields));
				gcpTransaction.put(entity.build());
			});
			var keys = gcpTransaction.commit().getGeneratedKeys();
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
				getDatastore().delete(createFactorKeys(c, idList));
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
		searchIndexer.index((Iterable<? extends Searchable>) items);
		return 0;
	}

	private Datastore getDatastore() {
		if (datastore == null) {
			synchronized (this) {
				if (datastore == null) {
					datastore = contextProvider.get() == Context.NONE
							? DatastoreOptions.getDefaultInstance().getService()
							: DatastoreOptions.newBuilder().setNamespace("test").build().getService();
				}
			}
		}

		return datastore;
	}

	private String getTableName(Class<?> c) {
		Context context = contextProvider.get();
		return (context == Context.NONE) ? c.getSimpleName() : String.format("%s.%s", context.name(), c.getSimpleName());
	}

	private FullEntity.Builder<?> createEntity(String kind, Object id) {
		Datastore datastore = getDatastore();
		if (id instanceof Long && (Long) id > 0L) {
			return FullEntity.newBuilder(datastore.newKeyFactory().setKind(kind).newKey((Long) id));
		} else if (id instanceof String) {
			return FullEntity.newBuilder(datastore.newKeyFactory().setKind(kind).newKey((String) id));
		}
		return FullEntity.newBuilder().setKey(datastore.newKeyFactory().setKind(kind).newKey());
	}

	private <T> T get(Class<T> c, Key key) {
		// try {
			return convert(c, getDatastore().get(key), ImmutableSet.of());
		/* } catch (EntityNotFoundException e) {
			throw new DD4StorageException(
					"Error fetching: " + getTableName(c) + ":" + (key.getId() > 0 ? key.getId() : key.getName()),
					e, ErrorCode.NOT_FOUND);
		} */
	}

	private Key createFactorKey(String kind, Object id) {
		return id instanceof Long
				? datastore.newKeyFactory().setKind(kind).newKey((Long) id)
				: datastore.newKeyFactory().setKind(kind).newKey(id.toString());
	}

	private <T,I> Key[] createFactorKeys(Class<T> c, Iterable<I> ids) {
		String kind = getTableName(c);

		ImmutableList<I> idList = ImmutableList.copyOf(ids);

		Key[] keys = new Key[idList.size()];
		for (int k = 0; k < idList.size(); k++) {
			keys[k] = createFactorKey(kind, idList.get(k));
		}
		return keys;
	}

	private <T, I> BulkGetable.MultiListResult<T, I> getEntities(Class<T> c, Iterable<I> ids) {
		return BulkGetable.MultiListResult.of(
				stream(getDatastore().get(createFactorKeys(c, ids)))
						.map(entity -> convert(c, entity, ImmutableSet.of()))
						.collect(toImmutableList()), ids);
	}

	private <T> QueryResult<T> listEntities(Class<T> c, Query.List query) {
		var eQuery = com.google.cloud.datastore.Query.newEntityQueryBuilder().setKind(getTableName(c));
		if (!query.getFilters().isEmpty()) {
			eQuery.setFilter(CompositeFilter.and(
					toPropertyFilter(c, query.getFilters().getFirst()),
					toPropertyFilters(c, query.getFilters())));
		}

		Integer limit = query.getLimit();
		if (limit == null || limit == 0) {
			limit = Integer.MAX_VALUE;
		}

		if (query.useDBSort()) {
			if (!query.getOrderBys().isEmpty()) {
				eQuery.setOrderBy(toOrderBy(query.getOrderBys().getFirst()), toOrderBys(query.getOrderBys()));
			}
			if (query.getLimit() != null && query.getLimit() > 0) {
				eQuery.setLimit(query.getLimit());
			}
			eQuery.setOffset(query.getOffset());
			ArrayList<T> results = new ArrayList<>();
			var queryResults = getDatastore().run(eQuery.build());
			while (queryResults.hasNext()) {
				results.add(convert(c, queryResults.next(), query.getFields()));
			}

			return QueryResult.of(c, results, results.size(), query);
		}


		ArrayList<T> allResults = new ArrayList<>();
		var queryResults = getDatastore().run(eQuery.build());
		while (queryResults.hasNext()) {
			allResults.add(convert(c, queryResults.next(), query.getFields()));
		}

		var comparator = getComparator(c, query);
		return QueryResult.of(c,
				allResults.stream().sorted(comparator)
						.skip(query.getOffset()).limit(limit).collect(toImmutableList()),
				allResults.size(), query);
	}

	private static OrderBy toOrderBy(Query.OrderBy orderBy) {
		return orderBy.getDesc() ? OrderBy.desc(orderBy.getColumn()) : OrderBy.asc(orderBy.getColumn());
	}

	private OrderBy[] toOrderBys(ImmutableList<Query.OrderBy> orderBys) {
		OrderBy[] result = new OrderBy[orderBys.size() - 1];
		for (int i = 1; i < orderBys.size(); i++) {
			result[i] = toOrderBy(orderBys.get(i));
		}
		return result;
	}

	private void setObject(
			FullEntity.Builder entity, JSONObject json, String fName, ImmutableMap<String, Field> fields) {
		if (fName.equals("id")) {
			return;
		}

		Field field = fields.get(fName);
		Object value = json.get(fName);
		if (value instanceof JSONArray && field.getType().getSimpleName().equals("byte[]")) {
			JSONArray jsonArray = json.getJSONArray(fName);
			byte[] bytes = new byte[jsonArray.length()];
			for (int b = 0; b < bytes.length; b++) {
				bytes[b] = (Byte) jsonArray.get(b);
			}
			entity.set(fName, Blob.copyFrom(bytes));
		} else if (value instanceof JSONObject || value instanceof JSONArray || value instanceof StringBuilder) {
			entity.set(fName, value.toString());
		} else if (value instanceof Enum) {
			entity.set(fName, value.toString());
		} else if (value instanceof DateTime dateTime) {
			entity.set(fName, Timestamp.ofTimeSecondsAndNanos(dateTime.getMillis() / 1000, 0));
		} else if (value instanceof Instant instant) {
			entity.set(fName, Timestamp.ofTimeSecondsAndNanos(instant.toEpochMilli() / 1000, 0));
		} else if (value instanceof Long && (fields.get(fName).getType() == DateTime.class
				|| fields.get(fName).getType() == Instant.class)) {
			entity.set(fName, Timestamp.ofTimeSecondsAndNanos(((Long) value) / 1000, 0));
		} else if (value instanceof Integer num) {
			entity.set(fName, num);
		} else if (value instanceof Long num) {
			entity.set(fName, num);
		} else if (value instanceof Double num) {
			entity.set(fName, num);
		} else if (value instanceof Float num) {
			entity.set(fName, num);
		} else {
			throw new RuntimeException("Unhandled datatype: " + value.getClass());
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

			Object val = value.get();

			/* TODO find out about Text
			if (val instanceof Text) {
				val = ((Text) val).getValue();
			} */

			try {

				switch (field.getType().getSimpleName()) {
					case "Boolean", "boolean" -> jsonObject.put(javaName, ((Boolean) val).booleanValue());
					case "ByteArray" -> jsonObject.put(javaName, val.toString().getBytes());
					case "byte[]", "Byte[]" -> jsonObject.put(javaName, ((Blob) val).toByteArray());
					case "DateTime" -> {
						if (val instanceof Date dateVal) {
							jsonObject.put(javaName, dateVal.getTime());
						} else {
							jsonObject.put(javaName,
									(val instanceof Long) ? (Long) val : DateTime.parse((String) val).getMillis());
						}
					}
					case "Double", "double" -> jsonObject.put(javaName, ((Double) val).doubleValue());
					case "Instant" -> {
						if (val instanceof Date dalVal) {
							jsonObject.put(javaName, dalVal.getTime());
						} else {
							jsonObject.put(javaName,
									(val instanceof Long) ? (Long) val : Instant.parse((String) val).toEpochMilli());
						}
					}
					case "Integer", "int" -> jsonObject.put(javaName, ((Long) val).intValue());
					case "Long", "long" -> {
						if (colName.endsWith("id")) {
							jsonObject.put(javaName, val);
						} else {
							jsonObject.put(javaName, val);
							// field.invokeSet(t, new java.sql.Timestamp((Long.parseLong(value.toString()))));
						}
					}
					case "StringBuilder" -> jsonObject.put(javaName, new StringBuilder((String) val));
					default -> {
						if (field.isCollection()) {
							jsonObject.put(javaName, new JSONArray((String) val));
						} else if (field.getType().isEnum()) {
							jsonObject.put(javaName, Enum.valueOf((Class<? extends Enum>) field.getType(), (String) val));
						} else {
							jsonObject.put(javaName, field.isObject() ? new JSONObject((String) val) : val);
						}
					}
				}
			} catch (ClassCastException cce) {
				throw new DD4StorageException("Error reading column: " + colName, cce);
			}
		});

		return JSONUtil.toObject(c, jsonObject);
	}

	private static PropertyFilter[] toPropertyFilters(Class<?> c, ImmutableList<Query.Filter> filters) {
		PropertyFilter[] propertyFilters = new PropertyFilter[filters.size() - 1];
		for (int f = 1; f < filters.size(); f++) {
			propertyFilters[f] = toPropertyFilter(c, filters.get(f));
		}
		return propertyFilters;
	}

	private static PropertyFilter toPropertyFilter(Class<?> c, Query.Filter filter) {
		String fieldName = FormatText.toLowerCamel(filter.getColumn());
		Field field = JSONUtil.getFields(c).get(fieldName);
		if (field == null) {
			throw new DD4StorageException("Unknown column: " + fieldName);
		}

		Value<?> value = null;
		ListValue listValue = null;
		if (filter.getValue() != null) {
			switch (field.getType().getSimpleName()) {
				case "Long":
				case "long":
				case "Integer":
				case "int": {
					if (filter.getValue() instanceof Collection<?> list) {
						ListValue.Builder listBuilder = ListValue.newBuilder();
						list.forEach(v -> listBuilder.addValue(Long.parseLong(v.toString())));
						listValue = listBuilder.build();
					} else {
						value = Value.fromPb(com.google.datastore.v1.Value.newBuilder()
								.setIntegerValue(Long.parseLong(filter.getValue().toString())).build());
					}
					break;
				}
				case "Boolean":
				case "boolean":
					value = Value.fromPb(com.google.datastore.v1.Value.newBuilder()
							.setBooleanValue(Boolean.parseBoolean(filter.getValue().toString())).build());
					break;
				case "Double":
				case "double":
				case "Float":
				case "float":
					value = Value.fromPb(com.google.datastore.v1.Value.newBuilder()
							.setDoubleValue(Double.parseDouble(filter.getValue().toString())).build());
					break;
				case "DateTime":
				case "Instant":
					long seconds = Long.parseLong(filter.getValue().toString());
					value = Value.fromPb(com.google.datastore.v1.Value.newBuilder()
							.setTimestampValue(Timestamp.ofTimeSecondsAndNanos(seconds, 0).toProto()).build());
					break;
				default: {
					if (filter.getValue() instanceof Collection<?> list) {
						ListValue.Builder listBuilder = ListValue.newBuilder();
						list.forEach(v -> listBuilder.addValue(v.toString()));
						listValue = listBuilder.build();
					} else {
						value = Value.fromPb(com.google.datastore.v1.Value.newBuilder()
								.setStringValue(filter.getValue().toString()).build());
					}
				}
			}
		}

		return switch (filter.getOperator()) {
			case "<" -> PropertyFilter.lt(fieldName, value);
			case "<=" -> PropertyFilter.le(fieldName, value);
			case "=", "" -> PropertyFilter.eq(fieldName, value);
			case "!=" -> PropertyFilter.neq(fieldName, value);
			case ">=" -> PropertyFilter.ge(fieldName, value);
			case ">" -> PropertyFilter.gt(fieldName, value);
			case "IN" -> PropertyFilter.in(fieldName, listValue);
			case "NOT_IN" -> PropertyFilter.not_in(fieldName, listValue);
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
