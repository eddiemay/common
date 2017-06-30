package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;

import java.util.List;
import java.util.function.UnaryOperator;

public interface DAO<T> {
	T getType();

	T create(T t) throws DD4StorageException;
	
	T get(int id) throws DD4StorageException;
	
	T update(int id, UnaryOperator<T> updater) throws DD4StorageException;
	
	boolean delete(int id) throws DD4StorageException;
	
	List<T> get(Filter... params) throws DD4StorageException;
	
	List<T> get(List<Filter> params) throws DD4StorageException;
	
	List<T> getAll() throws DD4StorageException;
}
