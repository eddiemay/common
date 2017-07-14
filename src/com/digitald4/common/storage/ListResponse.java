package com.digitald4.common.storage;

import java.util.ArrayList;
import java.util.List;

public class ListResponse<T> {
	protected List<T> items = new ArrayList<>();
	protected int totalSize;

	private ListResponse() {
	}

	private ListResponse(List<T> items, int totalSize) {
		this.items = items;
		this.totalSize = totalSize;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public List<T> getItemsList() {
		return items;
	}

	public int getItemsCount() {
		return items.size();
	}

	public static <T> Builder<T> newBuilder() {
		return new Builder<T>();
	}

	public static class Builder<T> extends ListResponse<T> {

		public Builder<T> setTotalSize(int totalSize) {
			this.totalSize = totalSize;
			return this;
		}

		public Builder<T> addAllItems(List<T> items) {
			this.items.addAll(items);
			return this;
		}

		public ListResponse<T> build() {
			return new ListResponse<T>(items, totalSize);
		}
	}
}
