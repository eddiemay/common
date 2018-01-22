package com.digitald4.common.storage;

import java.util.ArrayList;
import java.util.List;

public class QueryResult<T> extends ArrayList<T> {
	private final int totalSize;

	public QueryResult() {
		totalSize = 0;
	}

	public QueryResult(List<T> results, int totalSize) {
		super(results);
		this.totalSize = totalSize;
	}

	public int getTotalSize() {
		return totalSize;
	}
}
