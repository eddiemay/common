package com.digitald4.common.jpa;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class DD4SortedList<T> extends LinkedList<T> {
	private static final long serialVersionUID = -4189689439035154160L;
	public boolean add(T e){
		@SuppressWarnings("unchecked")
		int index = Collections.binarySearch((List<? extends Comparable<? super T>>)this, e);
		if (index < 0)
		    add(-index-1, e);
		return true;
	}
}
