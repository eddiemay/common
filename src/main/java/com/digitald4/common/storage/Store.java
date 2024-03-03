package com.digitald4.common.storage;

import com.digitald4.common.server.service.BulkGetable;
import com.google.common.collect.ImmutableList;

import java.util.function.UnaryOperator;

public interface Store<T, I> {
	Class<T> getTypeClass();

	T create(T t);

	ImmutableList<T> create(Iterable<T> entities);

	T get(I id);

	BulkGetable.MultiListResult<T, I> get(Iterable<I> ids);

	QueryResult<T> list(Query.List listQuery);

	T update(I id, UnaryOperator<T> updater);

	ImmutableList<T> update(Iterable<I> ids, UnaryOperator<T> updater);

	boolean delete(I id);

	int delete(Iterable<I> ids);
}
