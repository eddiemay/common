package com.digitald4.common.jdbc;

/**
 * Cache interface.
 * 
 * @param <K>
 * @param <V>
 */
public interface ICache<K, V> {

   void put(K key, V value);

   V get(K key);
   
   void delete(K key);
   
   void flush();
}
