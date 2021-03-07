package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.appengine.api.datastore.Query.SortDirection.ASCENDING;
import static com.google.appengine.api.datastore.Query.SortDirection.DESCENDING;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import org.json.JSONObject;

public class DAOCloudDS implements DAO {
	private static final Map<Class<?>, ImmutableMap<String, Field>> typeFields = new HashMap<>();
	private final DatastoreService datastoreService;

	@Inject
	public DAOCloudDS(DatastoreService datastoreService) {
		this.datastoreService = datastoreService;
	}

	@Override
	public <T> T create(T t) {
		return Calculate.executeWithRetries(2, () -> {
			JSONObject json = new JSONObject(t);
			Object id = json.has("id") ? json.get("id") : null;
			Entity entity = (id instanceof Long && (Long) id != 0L) ?
					new Entity(t.getClass().getSimpleName(), (Long) id) : new Entity(t.getClass().getSimpleName());
			json.keySet().forEach(field -> setObject(entity, json, field));

			return getFields(t.getClass()).get("id").invokeSet(t, datastoreService.put(entity).getId());
		});
	}

	@Override
	public <T> T get(Class<T> c, long id) {
		return Calculate.executeWithRetries(2, () -> get(c, KeyFactory.createKey(c.getSimpleName(), id)));
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query query) {
		return Calculate.executeWithRetries(2, () -> {
			QueryResult<Entity> queryResult = listEntities(c, query);
			return new QueryResult<>(
					queryResult.getResults().stream().map(entity -> convert(c, entity)).collect(toImmutableList()),
					queryResult.getTotalSize());
		});
	}

	@Override
	public <T> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return Calculate.executeWithRetries(2, () -> {
			T t = updater.apply(get(c, KeyFactory.createKey(c.getSimpleName(), id)));
			Entity entity = new Entity(c.getSimpleName(), id);
			JSONObject json = new JSONObject(t);
			json.keySet().forEach(field -> setObject(entity, json, field));
			datastoreService.put(entity);
			return t;
		});
	}

	@Override
	public <T> void delete(Class<T> c, long id) {
		Calculate.executeWithRetries(2, () -> {
			datastoreService.delete(KeyFactory.createKey(c.getSimpleName(), id));
			return true;
		});
	}

	@Override
	public <T> int delete(Class<T> c, Query query) {
		return Calculate.executeWithRetries(2, () -> {
			ImmutableList<Entity> results = listEntities(c, query).getResults();
			results.forEach(entity -> delete(c, entity.getKey().getId()));

			return results.size();
		});
	}

	private <T> T get(Class<T> c, Key key) {
		try {
			return convert(c, datastoreService.get(key));
		} catch (EntityNotFoundException e) {
			throw new DD4StorageException("Error fetching item: " + c.getSimpleName() + ":" + key.getId(), e);
		}
	}

	private QueryResult<Entity> listEntities(Class<?> c, Query query) {
		com.google.appengine.api.datastore.Query eQuery = new com.google.appengine.api.datastore.Query(c.getSimpleName());
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
		int end = query.getLimit() == 0 ? Integer.MAX_VALUE : query.getOffset() + query.getLimit();
		datastoreService.prepare(eQuery).asIterator().forEachRemaining(entity -> {
			if (count.getAndIncrement() >= query.getOffset() && count.get() <= end) {
				results.add(entity);
			}
		});

		return new QueryResult<>(results.build(), count.get());
	}

	private void setObject(Entity entity, JSONObject json, String fieldName) {
		if (fieldName.equals("id")) {
			return;
		}

		String colName = FormatText.toUnderScoreCase(fieldName);

		Object value = json.get(fieldName);
		if (value instanceof JSONObject) {
			entity.setProperty(colName, value.toString());
		} else {
			entity.setProperty(colName, value);
		}
	}

	private <T> T convert(Class<T> c, Entity entity) {
		if (entity == null) {
			return null;
		}

		ImmutableMap<String, Field> fieldMap = getFields(c);
		T t = DAORouterImpl.newInstance(c);
		Field idField = fieldMap.get("id");
		if (idField != null && idField.setMethod != null) {
			try {
				idField.setMethod.invoke(t, entity.getKey().getId());
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		entity.getProperties().forEach((colName, value) -> {
			Field field = fieldMap.get(colName);
			if (field == null) {
				throw new DD4StorageException("Unknown field: " + colName + " for Object: " + c.getSimpleName());
			}

			if (field.getSetMethod() == null) {
				return;
			}
			try {
				if (field.isObject()) {
					ObjectMapper mapper = new ObjectMapper();
					field.setMethod.invoke(t, mapper.readValue((String) value, field.getType()));
				} else {
					switch (field.getType().getSimpleName()) {
						case "ByteArray":
							field.setMethod.invoke(t, ByteString.copyFrom(value.toString().getBytes()));
							break;
						case "Integer":
						case "int":
							field.setMethod.invoke(t, ((Long) value).intValue());
							break;
						case "Long":
						case "long":
							if (colName.endsWith("id")) {
								field.setMethod.invoke(t, value);
							} else {
								field.setMethod.invoke(t, value);
								// field.setMethod.invoke(t, new java.sql.Timestamp((Long.parseLong(value.toString()))));
							}
							break;
						case "String":
						default:
							field.setMethod.invoke(t, value);
							break;
					}
				}
			} catch (Exception e) {
				throw new DD4StorageException(
						String.format(
								"%s for column: %s value: %s field type: %s", e.getMessage(), colName, value, field.getType()), e);
			}
		});

		return t;
	}

	private FilterPredicate convertToPropertyFilter(Class<?> c, Query.Filter filter) {
		String columName = filter.getColumn();
		Field field = getFields(c).get(columName);
		if (field == null) {
			throw new DD4StorageException("Unknown column: " + columName);
		}

		Object value;
		switch (field.getType().getSimpleName()) {
			case "Long":
			case "long":
				value = Long.parseLong(filter.getValue());
				break;
			case "Integer":
			case "int":
				value = Integer.parseInt(filter.getValue());
				break;
			case "Boolean":
			case "boolean":
				value = Boolean.parseBoolean(filter.getValue());
				break;
			case "Double":
			case "double":
				value = Double.parseDouble(filter.getValue());
				break;
			case "Float":
			case "float":
				value = Float.parseFloat(filter.getValue());
				break;
			default:
				value = filter.getValue();
		}

		switch (filter.getOperator()) {
			case "<":
				return new FilterPredicate(filter.getColumn(), FilterOperator.LESS_THAN, value);
			case "<=":
				return new FilterPredicate(filter.getColumn(), FilterOperator.LESS_THAN_OR_EQUAL, value);
			case "=":
			case "":
				return new FilterPredicate(filter.getColumn(), FilterOperator.EQUAL, value);
			case ">=":
				return new FilterPredicate(filter.getColumn(), FilterOperator.GREATER_THAN_OR_EQUAL, value);
			case ">":
				return new FilterPredicate(filter.getColumn(), FilterOperator.GREATER_THAN, value);
			default:
				throw new IllegalArgumentException("Unknown operator " + filter.getOperator());
		}
	}

	private static ImmutableMap<String, Field> getFields(Class<?> c) {
		return typeFields.computeIfAbsent(c, v -> {
			Map<String, Method> methods = new HashMap<>();
			stream(c.getMethods()).forEach(method -> methods.put(method.getName(), method));

			return methods.values().stream()
					.filter(
							method -> method.getParameters().length == 0
									&& (method.getName().startsWith("get") || method.getName().startsWith("is")))
					.map(method -> {
						String name = method.getName().substring(method.getName().startsWith("is") ? 2 : 3);
						return new Field(name, method.getReturnType(), methods.get("set" + name));
					})
					.collect(toImmutableMap(Field::getColName, identity()));
		});
	}

	private static class Field {
		private final String colName;
		private final Class<?> type;
		private final Method setMethod;

		public Field(String name, Class<?> type, Method setMethod) {
			this.colName = FormatText.toUnderScoreCase(name);
			this.type = type;
			this.setMethod = setMethod;
		}

		public String getColName() {
			return colName;
		}

		public Class<?> getType() {
			return type;
		}

		public Method getSetMethod() {
			return setMethod;
		}

		public <T> T invokeSet(T t, Object value) {
			try {
				setMethod.invoke(t, value);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			return t;
		}

		public boolean isObject() {
			return !getType().isPrimitive() && getType() != String.class;
		}
	}
}
