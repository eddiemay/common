package com.digitald4.common.storage;

import com.google.appengine.api.search.Document;

public interface SearchableStore<T> extends Store<T> {
  QueryResult<T> search(Query.Search searchQuery);
}
