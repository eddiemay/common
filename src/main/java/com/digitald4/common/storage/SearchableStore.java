package com.digitald4.common.storage;

import com.google.appengine.api.search.Index;

public interface SearchableStore<T, I> extends Store<T, I> {
  Index getIndex();

  QueryResult<T> search(Query.Search searchQuery);
}
