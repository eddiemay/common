package com.digitald4.common.jpa;

import java.util.List;
import java.util.TreeSet;

public class PropertyCollectionFactory<T> {
	
	private TreeSet<PropertyCollection<T>> collections = new TreeSet<PropertyCollection<T>>();
	
	public TreeSet<PropertyCollection<T>> getPropertyCollections(){
		return collections;
	}
	
	public List<T> getList(boolean create, DD4TypedQueryImpl<T> tq) throws Exception{
		PropertyCollection<T> crit = tq.getPropertyCollection();
		for (PropertyCollection<T> pc : collections) {
			if (pc.equals(crit)) {
				return pc.getList(create,tq);
			}
		}
		if (create) {
			PropertyCollection<T> pc = new PropertyCollection<T>(tq.isComplex(), crit.getKeys());
			collections.add(pc);
			return pc.getList(create, tq);
		}
		return null;
	}
	
	public boolean isEmpty(){
		return collections.isEmpty();
	}
	
	public void cache(T o, DD4TypedQueryImpl<T> tq) throws Exception {
		getList(true, tq).add(o);
	}
	
	public boolean cache(T o) {
		boolean result = false;
		for (PropertyCollection<T> pc : getPropertyCollections()) {
			if (pc.cache(o)) {
				result = true;
			}
		}
		return result;
	}
	
	public void evict(T o) {
		for (PropertyCollection<T> pc : getPropertyCollections()) {
			pc.evict(o);
		}
	}
}
