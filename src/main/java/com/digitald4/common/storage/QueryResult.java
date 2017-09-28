package com.digitald4.common.storage;

import java.util.ArrayList;
import java.util.List;

public class QueryResult<T> {
	private final List<T> results;
	private final int totalSize;

	private QueryResult(List<T> results, int totalSize) {
		this.results = results;
		this.totalSize = totalSize;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public List<T> getResultList() {
		return results;
	}

	public Builder<T> toBuilder() {
		return new Builder<>(results, totalSize);
	}

	public int getResultCount() {
		return results.size();
	}

	public static <T> Builder<T> newBuilder() {
		return new Builder<T>();
	}

	public static class Builder<T> {
		protected List<T> results = new ArrayList<>();
		protected int totalSize;

		private Builder() {};

		private Builder(List<T> results, int totalSize) {
			this.results = results;
			this.totalSize = totalSize;
		};

		public int getTotalSize() {
			return totalSize;
		}

		public List<T> getResultList() {
			return results;
		}

		public int getResultCount() {
			return results.size();
		}

		public Builder<T> setTotalSize(int totalSize) {
			this.totalSize = totalSize;
			return this;
		}

		public Builder<T> addResult(T result) {
			this.results.add(result);
			return this;
		}

		public Builder<T> addAllResult(List<T> results) {
			this.results.addAll(results);
			return this;
		}

		public Builder<T> setResultList(List<T> results) {
			this.results = results;
			return this;
		}

		public QueryResult<T> build() {
			return new QueryResult<T>(getResultList(), getTotalSize());
		}
	}
}
