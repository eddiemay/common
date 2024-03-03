package com.digitald4.common.storage;

import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;

public interface TypedDAO<R> {
	<T extends R> T create(T t);

	<T extends R> ImmutableList<T> create(Iterable<T> entities);

	<T extends R, I> T get(Class<T> c, I id);

	<T extends R, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids);

	<T extends R> QueryResult<T> list(Class<T> c, Query.List listQuery);

	<T extends Searchable> QueryResult<T> search(Class<T> c, Query.Search searchQuery);

	<T extends R, I> T update(Class<T> c, I id, UnaryOperator<T> updater);

	<T extends R, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater);

	<T extends R, I> boolean delete(Class<T> c, I id);

	<T extends R, I> int delete(Class<T> c, Iterable<I> ids);
}
