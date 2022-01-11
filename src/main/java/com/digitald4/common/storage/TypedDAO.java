package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;

public interface TypedDAO<R> {

	<T extends R> T create(T t);

	<T extends R> ImmutableList<T> create(Iterable<T> entities);

	<T extends R> T get(Class<T> c, long id);

	<T extends R> ImmutableList<T> get(Class<T> c, Iterable<Long> ids);

	<T extends R> QueryResult<T> list(Class<T> c, Query.List query);

	<T extends R> T update(Class<T> c, long id, UnaryOperator<T> updater);

	<T extends R> ImmutableList<T> update(Class<T> c, Iterable<Long> ids, UnaryOperator<T> updater);

	<T extends R> void delete(Class<T> c, long id);

	<T extends R> void delete(Class<T> c, Iterable<Long> ids);
}
