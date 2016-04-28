package com.digitald4.common.util;

import java.util.ArrayList;
import java.util.Collections;

public class SortedList<E extends Comparable<? super E>> extends ArrayList<E> {
	@Override
	public boolean add(E e) {
		if (contains(e)) {
			return false;
		}
		boolean ret = super.add(e);
		Collections.sort(this);
		return ret;
	}
}
