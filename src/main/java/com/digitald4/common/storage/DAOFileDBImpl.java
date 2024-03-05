package com.digitald4.common.storage;

import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class DAOFileDBImpl implements DAO {
  private static final String FIlE_FORMAT = "data/%s.db";
  protected final Map<Class<?>, DAOFileBasedImpl> itemDAOs = new HashMap<>();

  @Override
  public <T> T create(T t) {
    return getFileDAO(t.getClass()).create(t);
  }

  @Override
  public <T> ImmutableList<T> create(Iterable<T> entities) {
    return getFileDAO(entities.iterator().next().getClass()).create(entities);
  }

  @Override
  public <T, I> T get(Class<T> c, I id) {
    return getFileDAO(c).get(c, id);
  }

  @Override
  public <T, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids) {
    return getFileDAO(c).get(c, ids);
  }

  @Override
  public <T> QueryResult<T> list(Class<T> c, Query.List query) {
    return getFileDAO(c).list(c, query);
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Query.Search searchQuery) {
    return getFileDAO(c).search(c, searchQuery);
  }

  @Override
  public <T, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
    return getFileDAO(c).update(c, id, updater);
  }

  @Override
  public <T, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
    return getFileDAO(c).update(c, ids, updater);
  }

  @Override
  public <T, I> boolean delete(Class<T> c, I id) {
    return getFileDAO(c).delete(c, id);
  }

  @Override
  public <T, I> int delete(Class<T> c, Iterable<I> ids) {
    return getFileDAO(c).delete(c, ids);
  }

  private DAOFileBasedImpl getFileDAO(Class<?> cls) {
    itemDAOs.putIfAbsent(cls, new DAOFileBasedImpl(String.format(FIlE_FORMAT, cls.getSimpleName())).loadFromFile());
    return itemDAOs.get(cls);
  }

  public void saveFiles() {
    itemDAOs.values().forEach(DAOFileBasedImpl::saveToFile);
  }
}
