package com.digitald4.common.storage;

import java.util.ArrayList;
import java.util.List;

public class QueryResult<T> extends ArrayList<T> {
	private int totalSize;

	public int getTotalSize() {
		return totalSize;
	}

	public QueryResult<T> setTotalSize(int totalSize) {
		this.totalSize = totalSize;
		return this;
	}

	public QueryResult<T> setResultList(List<T>  results) {
		clear();
		addAll(results);
		return this;
	}
}
