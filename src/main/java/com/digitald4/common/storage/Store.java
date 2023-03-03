package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;

import java.util.function.UnaryOperator;

public interface Store<T, I> {
	Class<T> getTypeClass();

	T create(T t);

	ImmutableList<T> create(Iterable<T> entities);

	T get(I id);

	ImmutableList<T> get(Iterable<I> ids);

	QueryResult<T> list(Query.List listQuery);

	T update(I id, UnaryOperator<T> updater);

	ImmutableList<T> update(Iterable<I> ids, UnaryOperator<T> updater);

	void delete(I id);

	void delete(Iterable<I> ids);
}
