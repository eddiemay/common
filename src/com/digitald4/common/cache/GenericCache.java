package com.digitald4.common.cache;

public interface GenericCache<K, V> {
	
	boolean contains(K key);
	
	V get(K key);
	
	GenericCache<K, V> put(K key, V value);
	
	GenericCache<K, V> evict(K key);
	
	GenericCache<K, V> evictAll();
	
	GenericCache<K, V> clear();
}
