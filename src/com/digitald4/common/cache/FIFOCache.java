package com.digitald4.common.cache;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.annotations.VisibleForTesting;

public class FIFOCache<K, V> implements GenericCache<K, V> {
	private Map<K, V> map = new ConcurrentHashMap<K, V>();
	@VisibleForTesting Queue<K> queue = new ConcurrentLinkedQueue<K>();
	private int limit = 10;
	
	public FIFOCache() {
	}
	
	public FIFOCache(int limit) {
		this.limit = limit;
	}

	@Override
	public boolean contains(K key) {
		return map.containsKey(key);
	}

	@Override
	public V get(K key) {
		return map.get(key);
	}

	@Override
	public FIFOCache<K, V> put(K key, V value) {
		map.put(key, value);
		queue.add(key);
		if (queue.size() > limit) {
			map.remove(queue.remove());
		}
		return this;
	}

	@Override
	public FIFOCache<K, V> evict(K key) {
		map.remove(key);
		queue.remove(key);
		return this;
	}

	@Override
	public FIFOCache<K, V> evictAll() {
		return clear();
	}

	@Override
	public FIFOCache<K, V> clear() {
		map.clear();
		queue.clear();
		return this;
	}
}
