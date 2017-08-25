package com.digitald4.common.jpa;

import javax.persistence.TypedQuery;

public class DD4CacheImplV2 implements DD4Cache {
	private final DD4EntityManagerFactory emf;
	private DD4Hashtable<Class<?>, DD4Hashtable<String, Object>> hashById = new DD4Hashtable<>(199);
	private DD4Hashtable<Class<?>, DD4Hashtable<String, DD4TypedQueryImplV2<?>>> queries = new DD4Hashtable<>();

	DD4CacheImplV2(DD4EntityManagerFactory emf) {
		this.emf = emf;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public boolean contains(Class c, Object o) {
		DD4Hashtable<String, Object> classHash = hashById.get(c);
		return (classHash != null && classHash.containsKey(((Entity)o).getHashKey()));
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void evict(Class c) {
		hashById.remove(c);
		queries.remove(c);
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void evict(Class c, Object o) {
		DD4Hashtable<String, Object> classHash = hashById.get(c);
		if (classHash != null) { 
			classHash.remove(((Entity)o).getHashKey());
		}
		DD4Hashtable<String, DD4TypedQueryImplV2<?>> queryHash = queries.get(c);
		if (queryHash != null) { 
			for (DD4TypedQueryImplV2<?> query : queryHash.values()) {
				query.evict(o);
			}
		}
	}
	
	@Override
	public void evictAll() {
		hashById.clear();
		queries.clear();
	}
	
	@Override
	public <T> T find(Class<T> c, PrimaryKey pk) throws Exception {
		return getCachedObj(c, pk);
	}
	
	@Override
	public <T> void reCache(T o) {
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) o.getClass();
		DD4Hashtable<String, DD4TypedQueryImplV2<?>> queryHash = queries.get(c);
		if (queryHash != null) { 
			for (DD4TypedQueryImplV2<?> query : queryHash.values()) {
				query.evict(o);
				((DD4TypedQueryImplV2<T>) query).cache(o);
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCachedObj(Class<T> c, Object o) {
		DD4Hashtable<String,Object> classHash = hashById.get(c);
		if (classHash == null) { 
			return null;
		}
		return (T)classHash.get(((Entity)o).getHashKey());
	}
	
	@Override
	public <T> void put(T o) {
		DD4Hashtable<String, Object> classHash = hashById.get(o.getClass());
		if (classHash == null) {
			classHash = new DD4Hashtable<String, Object>(199);
			hashById.put(o.getClass(), classHash);
		}
		classHash.put(((Entity)o).getHashKey(), o);
	}

	@Override
	public <T> TypedQuery<T> createQuery(String query, Class<T> c) {
		DD4Hashtable<String, DD4TypedQueryImplV2<?>> queryHash = getQueryHash(c, true);
		@SuppressWarnings("unchecked")
		DD4TypedQueryImplV2<T> cached = (DD4TypedQueryImplV2<T>)queryHash.get(query);
		if (cached != null) {
			return new DD4TypedQueryImplV2<T>(cached);
		}
		cached = new DD4TypedQueryImplV2<T>(emf.createEntityManager(), null, query, c);
		queryHash.put(query, cached);
		return cached;
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> c) { 
		String key = c.getName() + "." + name;
		DD4Hashtable<String, DD4TypedQueryImplV2<?>> queryHash = getQueryHash(c, true);
		@SuppressWarnings("unchecked")
		DD4TypedQueryImplV2<T> cached = (DD4TypedQueryImplV2<T>)queryHash.get(key);
		if (cached != null) {
			return new DD4TypedQueryImplV2<T>(cached);
		}
		cached = new DD4TypedQueryImplV2<T>(emf.createEntityManager(), name, null, c);
		queryHash.put(key, cached);
		return cached;
	}

	private <T> DD4Hashtable<String, DD4TypedQueryImplV2<?>> getQueryHash(Class<T> c, boolean create) {
		DD4Hashtable<String, DD4TypedQueryImplV2<?>> queryHash = (queries.get(c));
		if (queryHash == null && create) {
			queryHash = new DD4Hashtable<String, DD4TypedQueryImplV2<?>>();
			queries.put(c, queryHash);
		}
		return queryHash;
	}
}
