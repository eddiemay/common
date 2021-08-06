package com.digitald4.common.storage;

import java.util.function.UnaryOperator;

public interface Store<T> {
	T getType();

	T create(T t);

	T get(long id);

	QueryResult<T> list(Query query);

	T update(long id, UnaryOperator<T> updater);

	void delete(long id);

	int delete(Iterable<Long> ids);
}
