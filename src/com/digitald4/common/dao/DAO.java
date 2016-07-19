package com.digitald4.common.dao;

import java.util.List;

import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.google.protobuf.Descriptors.Descriptor;

public interface DAO<T> {
	Descriptor getDescriptor();
	
	T create(T t) throws DD4StorageException;
	
	T get(int id) throws DD4StorageException;
	
	T update(int id, Function<T, T> updater) throws DD4StorageException;
	
	void delete(int id) throws DD4StorageException;
	
	List<T> get(QueryParam... params) throws DD4StorageException;
	
	List<T> get(List<QueryParam> params) throws DD4StorageException;
	
	List<T> getAll() throws DD4StorageException;
}
