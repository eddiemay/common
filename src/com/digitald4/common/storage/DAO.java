package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import java.util.function.UnaryOperator;

public interface DAO<T> {
	T getType();

	T create(T t) throws DD4StorageException;
	
	T get(int id) throws DD4StorageException;

	ListResponse<T> list(ListRequest listRequest) throws DD4StorageException;
	
	T update(int id, UnaryOperator<T> updater) throws DD4StorageException;
	
	void delete(int id) throws DD4StorageException;
}
