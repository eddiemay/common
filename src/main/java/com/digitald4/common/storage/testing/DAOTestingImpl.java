package com.digitald4.common.storage.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.json.JSONObject;

public class DAOTestingImpl implements DAO {
	private final AtomicLong idGenerator = new AtomicLong(5000);
	private final Map<Class<?>, Map<Long, JSONObject>> tables = new HashMap<>();

	@Override
	public <T> T create(T t) {
		Map<Long, JSONObject> table = tables.computeIfAbsent(t.getClass(), c -> new HashMap<>());
		JSONObject jsonObject = JSONUtil.toJSON(t);
		long id = jsonObject.optLong("id");
		if (id == 0L) {
			jsonObject.put("id", id = idGenerator.incrementAndGet());
		}
		table.put(id, jsonObject);
		return JSONUtil.toObject((Class<T>) t.getClass(), jsonObject);
	}

	@Override
	public <T> T get(Class<T> c, long id) {
		Map<Long, JSONObject> table = tables.get(c);
		if (table == null) {
			return null;
		}

		return JSONUtil.toObject(c, table.get(id));
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query query) {
		Map<Long, JSONObject> table = tables.get(c);
		if (table != null) {
			ImmutableList<JSONObject> results = ImmutableList.copyOf(table.values());
			for (Filter filter : query.getFilters()) {
				String field = filter.getColumn();
				results = results.parallelStream()
						.filter(json -> json.opt(field).equals(filter.getValue()))
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

			if (query.getLimit() > 0 && results.size() > query.getLimit()) {
				results = results.subList(0, query.getLimit());
			}

			return new QueryResult<>(
					results.stream().map(json -> JSONUtil.toObject(c, json)).collect(toImmutableList()), totalSize);
		}

		return new QueryResult<>(ImmutableList.of(), 0);
	}

	@Override
	public <T> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		T t = get(c, id);
		if (t != null) {
			t = updater.apply(t);
			Map<Long, JSONObject> table = tables.get(c);
			table.put(id, JSONUtil.toJSON(t));
		}

		return t;
	}

	@Override
	public <T> void delete(Class<T> c, long id) {
		Map<Long, JSONObject> table = tables.get(c);
		if (table != null) {
			table.remove(id);
		}
	}

	@Override
	public <T> int delete(Class<T> c, Iterable<Long> ids) {
		Map<Long, JSONObject> table = tables.get(c);
		if (table != null) {
			return (int) stream(ids).filter(id -> table.remove(id) != null).count();
		}

		return 0;
	}
}
