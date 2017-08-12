package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import java.util.function.UnaryOperator;

public interface DAO<T> {
	T getType();

	T create(T t);
	
	T get(int id);

	ListResponse<T> list(ListRequest listRequest);
	
	T update(int id, UnaryOperator<T> updater);
	
	void delete(int id);
}
