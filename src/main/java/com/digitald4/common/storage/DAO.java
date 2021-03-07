package com.digitald4.common.storage;

import java.util.function.UnaryOperator;

public interface DAO extends TypedDAO<Object> {

  <T> T create(T t);

  <T> T get(Class<T> c, long id);

  <T> QueryResult<T> list(Class<T> c, Query query);

  <T> T update(Class<T> c, long id, UnaryOperator<T> updater);

  <T> void delete(Class<T> c, long id);
}
