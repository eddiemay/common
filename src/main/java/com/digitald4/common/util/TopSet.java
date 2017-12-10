package com.digitald4.common.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class TopSet<T> extends TreeSet<T> {
	private final Comparator<T> comparator;
	private final int limit;

	public TopSet(int limit, Comparator<T> comparator) {
		super(comparator);
		this.comparator = comparator;
		this.limit = limit;
	}

	@Override
	public boolean add(T t) {
		if (size() < limit) {
			super.add(t);
			return true;
		}
		T last = last();
		if (comparator.compare(t, last) < 0) {
			super.add(t);
			remove(last);
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends T> items) {
		items.forEach(this::add);
		return true;
	}

	public boolean addSorted(Collection<? extends T> items) {
		for (T item : items) {
			if (!add(item)) {
				return false;
			}
		}
		return true;
	}
}
