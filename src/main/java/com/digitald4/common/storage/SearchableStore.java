package com.digitald4.common.storage;

import com.digitald4.common.model.Searchable;

public interface SearchableStore<T extends Searchable, I> extends Store<T, I> {
  QueryResult<T> search(Query.Search searchQuery);
}
