package com.digitald4.common.storage;

import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;

public class QueryResult<T> {
	private final ImmutableList<T> items;
	private final int totalSize;
	private final Query query;

	protected QueryResult(Iterable<T> items, int totalSize, Query query) {
		this.items = ImmutableList.copyOf(items);
		this.totalSize = totalSize;
		this.query = query;
	}

	public static <T> QueryResult<T> of(Iterable<T> results, int totalSize, Query query) {
		return new QueryResult<>(results, totalSize, query);
	}

	public ImmutableList<T> getItems() {
		return items;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public Query query() {
		return query;
	}

	public String getFilter() {
		if (query == null || !(query instanceof Query.List)) {
			return null;
		}

		return ((Query.List) query).getFilters().stream()
				.map(f -> String.format("%s%s%s", f.getColumn(), f.getOperator(), f.getValue())).collect(joining(","));
	}

	public String getOrderBy() {
		return query == null ? null
				: query.getOrderBys().stream().map(ob -> ob.getColumn() + (ob.getDesc() ? " DESC" : "")).collect(joining(","));
	}

	public Integer getPageSize() {
		return query == null ? 0 : query.getPageSize();
	}

	public int getPageToken() {
		return query == null ? 0 : query.getPageToken();
	}
}
