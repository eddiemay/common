package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import java.util.function.UnaryOperator;

public interface DAO<T> {
	T getType();

	T create(T t);
	
	T get(long id);

	ListResponse<T> list(ListRequest listRequest);
	
	T update(long id, UnaryOperator<T> updater);
	
	void delete(long id);
}
