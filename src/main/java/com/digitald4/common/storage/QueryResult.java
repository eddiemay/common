package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;

public class QueryResult<T> {
	private final ImmutableList<T> results;
	private final int totalSize;
	private final String filter;
	private final String orderBy;
	private final int pageSize;
	private final int pageToken;

	private QueryResult(
			Iterable<T> results, int totalSize, String filter, String orderBy, int pageSize, int pageToken) {
		this.results = ImmutableList.copyOf(results);
		this.totalSize = totalSize;
		this.filter = filter;
		this.orderBy = orderBy;
		this.pageSize = pageSize;
		this.pageToken = pageToken;
	}

	public static <T> QueryResult<T> of(Iterable<T> results, int totalSize, Query query) {
		return new QueryResult<>(
				results,
				totalSize,
				query.getFilters().stream()
						.map(f -> String.format("%s%s%s", f.getColumn(), f.getOperator(), f.getValue())).collect(joining(",")),
				query.getOrderBys().stream().map(ob -> ob.getColumn() + (ob.getDesc() ? " DESC" : "")).collect(joining(",")),
				query.getLimit(),
				query.getOffset());
	}

	public static <I, T> QueryResult<T> transform(QueryResult<I> queryResult, Function<I, T> function) {
		return new QueryResult<>(
				queryResult.getResults().stream().map(function).collect(toImmutableList()),
				queryResult.getTotalSize(),
				queryResult.getFilter(),
				queryResult.getOrderBy(),
				queryResult.getPageSize(),
				queryResult.getPageToken());
	}

	public ImmutableList<T> getResults() {
		return results;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public String getFilter() {
		return filter;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getPageToken() {
		return pageToken;
	}
}
