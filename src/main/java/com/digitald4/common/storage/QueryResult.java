package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class QueryResult<T> {
	private final ImmutableList results;
	private final int totalSize;

	public QueryResult(List<T> results, int totalSize) {
		this.results = ImmutableList.copyOf(results);
		this.totalSize = totalSize;
	}

	public ImmutableList<T> getResults() {
		return results;
	}

	public int getTotalSize() {
		return totalSize;
	}
}
