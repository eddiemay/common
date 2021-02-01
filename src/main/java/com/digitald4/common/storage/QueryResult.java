package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class QueryResult<T> {
	private final ImmutableList<T> results;
	private final int totalSize;

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
}
