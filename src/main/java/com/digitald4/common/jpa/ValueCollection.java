package com.digitald4.common.jpa;

import java.util.Collections;
import java.util.List;

public class ValueCollection<T> extends PrimaryKey<Object> {
	
	private final PropertyCollection<T> pc;
	private List<T> list = Collections.synchronizedList(new DD4SortedList<T>());
	
	public ValueCollection(PropertyCollection<T> pc, Object... values) {
		super(values);
		this.pc = pc;
	}
	
	public List<T> getList() {
		return list;
	}
	
	public boolean meetsCriteria(Object[] values) {
		int i = 0;
		for (Object value : values) {
			if (!pc.getKeys()[i].getRight().evaluate(value, getKeys()[i])) {
				return false;
			}
			i++;
		}
		return true;
	}
}
