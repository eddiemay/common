package com.digitald4.common.storage;

import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.Query.Search;

public class SearchIndexerFakeImpl implements SearchIndexer {

  @Override
  public <T extends Searchable> int index(Iterable<T> entities) {
    return 0;
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
    return null;
  }

  @Override
  public <T extends Searchable> void removeIndex(Class<T> c, Iterable<?> ids) {}
}
