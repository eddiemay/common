package com.digitald4.common.storage.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.Query.Search;
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
	private final Map<Class<?>, Map<String, JSONObject>> tables = new HashMap<>();

	@Override
	public <T> T create(T t) {
		Map<String, JSONObject> table = tables.computeIfAbsent(t.getClass(), c -> new HashMap<>());
		JSONObject jsonObject = JSONUtil.toJSON(t);
		Object id = jsonObject.opt("id");
		if (id == null || id instanceof Long && (long) id == 0L) {
			jsonObject.put("id", id = idGenerator.incrementAndGet());
		}
		table.put(id.toString(), jsonObject);
		return JSONUtil.toObject((Class<T>) t.getClass(), jsonObject);
	}

	@Override
	public <T> ImmutableList<T> create(Iterable<T> entities) {
		return stream(entities).map(this::create).collect(toImmutableList());
	}

	@Override
	public <T, I> T get(Class<T> c, I id) {
		Map<String, JSONObject> table = tables.get(c);
		if (table == null) {
			return null;
		}

		return JSONUtil.toObject(c, table.get(id.toString()));
	}

	@Override
	public <T, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
		return stream(ids).map(id -> get(c, id)).collect(toImmutableList());
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query.List query) {
		Map<String, JSONObject> table = tables.get(c);
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

			if (query.getLimit() != null && query.getLimit() > 0 && results.size() > query.getLimit()) {
				results = results.subList(0, query.getLimit());
			}

			return QueryResult.of(
					results.stream().map(json -> JSONUtil.toObject(c, json)).collect(toImmutableList()), totalSize, query);
		}

		return QueryResult.of(ImmutableList.of(), 0, query);
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
			tables.get(c).put(id.toString(), JSONUtil.toJSON(t));
		}

		return t;
	}

	@Override
	public <T, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
		return stream(ids).map(id -> update(c, id, updater)).collect(toImmutableList());
	}

	@Override
	public <T, I> void delete(Class<T> c, I id) {
		Map<String, JSONObject> table = tables.get(c);
		if (table != null) {
			table.remove(id.toString());
		}
	}

	@Override
	public <T, I> void delete(Class<T> c, Iterable<I> ids) {
		Map<String, JSONObject> table = tables.get(c);
		if (table != null) {
			stream(ids).map(Object::toString).forEach(table::remove);
		}
	}
}
