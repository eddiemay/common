package com.digitald4.common.storage;

import com.digitald4.common.model.Searchable;

import javax.inject.Provider;

public abstract class SearchableStoreImpl<T extends Searchable, I> extends GenericStore<T, I>
    implements SearchableStore<T, I> {
  private final Class<T> t;
  private final Provider<DAO> daoProvider;

  public SearchableStoreImpl(Class<T> t, Provider<DAO> daoProvider) {
    super(t, daoProvider);
    this.t = t;
    this.daoProvider = daoProvider;
  }

  @Override
  public QueryResult<T> search(Query.Search searchQuery) {
    return daoProvider.get().search(t, searchQuery);
  }
}
