package com.digitald4.common.storage;

import java.util.function.UnaryOperator;

public interface TypedDAO<R> {

	<T extends R> T create(T t);

	<T extends R> T get(Class<T> c, long id);

	<T extends R> QueryResult<T> list(Class<T> c, Query query);

	<T extends R> T update(Class<T> c, long id, UnaryOperator<T> updater);

	<T extends R> void delete(Class<T> c, long id);

	<T extends R> int delete(Class<T> c, Query query);
}
