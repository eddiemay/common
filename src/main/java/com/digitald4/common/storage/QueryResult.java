package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class QueryResult<T> {
	private final ImmutableList<T> results;
	private final int totalSize;
	private String filter;
	private String orderBy;
	private int pageSize;
	private int pageToken;

	public QueryResult(Iterable<T> results, int totalSize) {
		this.results = ImmutableList.copyOf(results);
		this.totalSize = totalSize;
	}

	public QueryResult(Iterable<T> results) {
		this(results, Iterables.size(results));
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

	public QueryResult<T> setFilter(String filter) {
		this.filter = filter;
		return this;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public QueryResult<T> setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public int getPageSize() {
		return pageSize;
	}

	public QueryResult<T> setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public int getPageToken() {
		return pageToken;
	}

	public QueryResult<T> setPageToken(int pageToken) {
		this.pageToken = pageToken;
		return this;
	}
}
