package com.digitald4.common.storage;

import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.Query.Search;

public interface SearchIndexer {
  <T extends Searchable> void index(Iterable<T> enities);

  <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery);

  <T extends Searchable> void removeIndex(Class<T> c, Iterable<?> ids);
}
