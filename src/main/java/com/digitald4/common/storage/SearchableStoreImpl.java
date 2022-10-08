package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.google.appengine.api.search.*;
import com.google.appengine.api.search.SortExpression.SortDirection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.inject.Provider;

public abstract class SearchableStoreImpl<T, I> extends GenericStore<T, I> implements SearchableStore<T, I> {

  private static final int UPDATE_LIMIT = 200;

  private final Index index;
  public SearchableStoreImpl(Class<T> t, Provider<DAO> daoProvider, Index index) {
    super(t, daoProvider);
    this.index = index;
  }

  @Override
  public Index getIndex() {
    return index;
  }

  @Override
  public QueryResult<T> search(Query.Search searchQuery) {
    SortOptions.Builder sortOptions = SortOptions.newBuilder();
    searchQuery.getOrderBys().forEach(
        orderBy -> sortOptions.addSortExpression(
            SortExpression.newBuilder()
                .setExpression(orderBy.getColumn())
                .setDirection(orderBy.getDesc() ? SortDirection.DESCENDING : SortDirection.ASCENDING)));

    Results<ScoredDocument> results = index.search(
        com.google.appengine.api.search.Query.newBuilder()
            .setOptions(
                QueryOptions.newBuilder()
                    .setSortOptions(sortOptions)
                    .setLimit(searchQuery.getLimit())
                    .setOffset(searchQuery.getOffset()))
            .build(searchQuery.getSearchText()));

    ImmutableList<T> ts = results.getResults().stream()
        .map(this::fromDocument)
        .collect(toImmutableList());

    return QueryResult.of(ts, (int) results.getNumberFound(), searchQuery);
  }

  @Override
  protected ImmutableList<T> postprocess(ImmutableList<T> entities) {
    return reindex(entities);
  }

  public ImmutableList<T> reindex(ImmutableList<T> entities) {
    if (index != null) {
      for (int x = 0; x < entities.size(); x += UPDATE_LIMIT) {
        index.putAsync(entities.stream().skip(x).limit(UPDATE_LIMIT).map(this::toDocument).collect(toImmutableList()));
      }
    }

    return entities;
  }

  @Override
  protected void postdelete(Iterable<I> ids) {
    removeIndex(stream(ids).map(String::valueOf).collect(toImmutableList()));
  }

  public void removeIndex(Iterable<String> documentIds) {
    if (index != null) {
      for (int x = 0; x < Iterables.size(documentIds); x += UPDATE_LIMIT) {
        index.delete(stream(documentIds).skip(x).limit(UPDATE_LIMIT).collect(toImmutableList()));
      }
    }
  }

  public abstract Document toDocument(T t);

  public abstract T fromDocument(Document document);
}
