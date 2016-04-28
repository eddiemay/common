package com.digitald4.common.jpa;

import javax.persistence.Cache;
import javax.persistence.TypedQuery;

public interface DD4Cache extends Cache {
	enum NULL_TYPES{IS_NULL, IS_NOT_NULL};
	
	public <T> T find(Class<T> c, PrimaryKey pk) throws Exception;
	
	public <T> T getCachedObj(Class<T> c, Object o);
	
	public <T> void put(T o);
	
	public <T> void reCache(T o);
	
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> c);
	
	public <T> TypedQuery<T> createQuery(String jpql, Class<T> c);
}
