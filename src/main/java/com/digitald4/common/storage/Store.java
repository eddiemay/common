package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;

import java.util.function.UnaryOperator;

public interface Store<T> {
	T getType();

	T create(T t);

	ImmutableList<T> create(Iterable<T> entities);

	T get(long id);

	ImmutableList<T> get(Iterable<Long> ids);

	QueryResult<T> list(Query query);

	T update(long id, UnaryOperator<T> updater);

	ImmutableList<T> update(Iterable<Long> ids, UnaryOperator<T> updater);

	void delete(long id);

	void delete(Iterable<Long> ids);
}
