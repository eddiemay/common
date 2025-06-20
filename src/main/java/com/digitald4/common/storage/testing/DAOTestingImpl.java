package com.digitald4.common.storage.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;

public class DAOTestingImpl implements DAO {
	private final AtomicLong idGenerator = new AtomicLong(5000);
	private final Map<String, JSONObject> items = new HashMap<>();

	private final ChangeTracker changeTracker;

	public DAOTestingImpl(ChangeTracker changeTracker) {
		this.changeTracker = changeTracker;
	}

	@Override
	public <T> Transaction<T> persist(Transaction<T> transaction) {
		changeTracker.prePersist(this, transaction);
		transaction.getOps().forEach(op -> {
			JSONObject json = JSONUtil.toJSON(op.getEntity());
			Object id = json.opt("id");
			if (id == null || id instanceof Number && (long) id == 0L) {
				json.put("id", id = idGenerator.incrementAndGet());
			}
			items.put(getIdString(op.getEntity().getClass(), id), json);
			op.setEntity(JSONUtil.toObject(op.getTypeClass(), json));
		});
		changeTracker.postPersist(this, transaction);
		return transaction;
	}

	@Override
	public <T, I> T get(Class<T> c, I id) {
		return JSONUtil.toObject(c, items.get(getIdString(c, id)));
	}

	@Override
	public <T, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids) {
		return BulkGetable.MultiListResult.of(
				stream(ids).map(id -> get(c, id)).filter(Objects::nonNull).collect(toImmutableList()), ids);
	}

	@Override
	public <T> QueryResult<T> list(Class<T> c, Query.List query) {
		ImmutableList<JSONObject> results = items.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(c.getSimpleName()))
				.map(Entry::getValue)
				.collect(toImmutableList());

		for (Filter filter : query.getFilters()) {
			String field = filter.getColumn();
			results = results.parallelStream()
					.filter(json -> json.opt(field).equals(filter.getValue()))
					.collect(toImmutableList());
		}

		for (OrderBy orderBy : query.getOrderBys()) {
			String field = orderBy.getColumn();
			results = results.stream()
					.sorted(
							(json1, json2) -> ((Comparable<Object>) json1.get(field)).compareTo(json2.get(field)))
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
				c, results.stream().map(json -> JSONUtil.toObject(c, json)).collect(toImmutableList()), totalSize, query);
	}

	@Override
	public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
		throw new DD4StorageException("Unimplemented method", ErrorCode.BAD_REQUEST);
	}

	@Override
	public <T, I> boolean delete(Class<T> c, I id) {
		return items.remove(getIdString(c, id)) != null;
	}

	@Override
	public <T, I> int delete(Class<T> c, Iterable<I> ids) {
		return (int) stream(ids).map(id -> delete(c, id)).filter(Boolean::booleanValue).count();
	}

	private <T> String getIdString(Class<T> c, Object id) {
		return String.format("%s-%s", c.getSimpleName(), id);
	}
}
