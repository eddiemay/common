package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import java.util.function.UnaryOperator;

public interface Store<T> {
	T getType();

	T create(T t);

	T get(long id);

	QueryResult<T> list(Query query);

	T update(long id, UnaryOperator<T> updater);

	void delete(long id);
}
