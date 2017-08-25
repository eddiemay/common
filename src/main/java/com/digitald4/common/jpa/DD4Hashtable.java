package com.digitald4.common.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DD4Hashtable<K, V> {

	private Map<K,V> cache;
	/**
	 * Creates a new ESPHashtable with the specified parameters. 
	 * @param cacheName The name of this ESPHashtable
	 */
	public DD4Hashtable() {
		cache =  Collections.synchronizedMap(new HashMap<K,V>());
	}

	public DD4Hashtable(int intitialCapacity) {
		cache =  Collections.synchronizedMap(new HashMap<K,V>(intitialCapacity));
	}

	/**
	 * Adds the key-value pair to this cache. Entry will expire from cache after the default maxLife.
	 * Logs a message if the cache throws an exception.
	 * 
	 * @param key The key.
	 * @param value The value.
	 */
	public void put(K key, V value) {
		cache.put(key, value);
	}

	public V get(K key){
		return cache.get(key);
	}

	public void clear(){
		cache.clear();
	}

	public void remove(K key){
		cache.remove(key);
	}

	public int size(){
		return cache.size();
	}
	
	public boolean containsKey(Object key){
		return cache.containsKey(key);
	}
	
	public Collection<V> values() {
		return cache.values();
	}
}