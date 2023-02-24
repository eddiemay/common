package com.digitald4.common.storage;

import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;

import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.storage.Annotations.DefaultDAO;
import com.digitald4.common.storage.Query.List;
import com.digitald4.common.storage.Query.Search;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import org.joda.time.DateTime;

public class DAOHelper implements DAO {
  private final DAO dao;
  private final Clock clock;
  private final SearchIndexer searchIndexer;

  @Inject
  public DAOHelper(@DefaultDAO DAO dao, Clock clock, SearchIndexer searchIndexer) {
    this.dao = dao;
    this.clock = clock;
    this.searchIndexer = searchIndexer;
  }

  @Override
  public <T> T create(T t) {
    if (t instanceof HasModificationTimes) {
      DateTime now = new DateTime(clock.millis());
      ((HasModificationTimes) t).setCreationTime(now).setLastModifiedTime(now);
    }

    t = dao.create(t);

    if (t instanceof Searchable) {
      searchIndexer.index(ImmutableList.of((Searchable) t));
    }

    return t;
  }

  @Override
  public <T> ImmutableList<T> create(Iterable<T> entities) {
    if (!entities.iterator().hasNext()) {
      return ImmutableList.of();
    }

    if (entities.iterator().next() instanceof HasModificationTimes) {
      DateTime now = new DateTime(clock.millis());
      stream(entities).map(t -> (HasModificationTimes) t)
          .forEach(t -> {
            if (t.getCreationTime() == null) {
              t.setCreationTime(now);
            }
            t.setLastModifiedTime(now);
          });
    }

    ImmutableList<T> created = dao.create(entities);

    if (created.get(0) instanceof Searchable) {
      searchIndexer.index((ImmutableList<? extends Searchable>) created);
    }

    return created;
  }

  @Override
  public <T, I> T get(Class<T> c, I id) {
    return dao.get(c, id);
  }

  @Override
  public <T, I> ImmutableList<T> get(Class<T> c, Iterable<I> ids) {
    return dao.get(c, ids);
  }

  @Override
  public <T> QueryResult<T> list(Class<T> c, List listQuery) {
    return dao.list(c, listQuery);
  }

  @Override
  public <T extends Searchable> QueryResult<T> search(Class<T> c, Search searchQuery) {
    return dao.search(c, searchQuery);
  }

  @Override
  public <T, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
    T updated = dao.update(c, id, current -> {
      current = updater.apply(current);
      if (current instanceof HasModificationTimes) {
        ((HasModificationTimes) current).setLastModifiedTime(new DateTime(clock.millis()));
      }
      return current;
    });

    if (updated instanceof Searchable) {
      searchIndexer.index(ImmutableList.of((Searchable) updated));
    }

    return updated;
  }

  @Override
  public <T, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
    if (!ids.iterator().hasNext()) {
      return ImmutableList.of();
    }
    final DateTime now = new DateTime(clock.millis());

    UnaryOperator<T> updater_ =
        stream(c.getInterfaces()).noneMatch(i -> i.getSimpleName().equals("HasModificationTimes"))
            ? updater
            : current -> {
              current = updater.apply(current);
              ((HasModificationTimes) current).setLastModifiedTime(now);
              return current;
            };

    ImmutableList<T> updated = dao.update(c, ids, updater_);
    if (updated.get(0) instanceof Searchable) {
      searchIndexer.index((ImmutableList<? extends Searchable>) updated);
    }

    return updated;
  }

  @Override
  public <T, I> void delete(Class<T> c, I id) {
    dao.delete(c, id);
    if (stream(c.getInterfaces()).anyMatch(i -> i.getSimpleName().equals("Searchable"))) {
      searchIndexer.removeIndex((Class<? extends Searchable>) c, ImmutableList.of(id));
    }
  }

  @Override
  public <T, I> void delete(Class<T> c, Iterable<I> ids) {
    dao.delete(c, ids);
    if (stream(c.getInterfaces()).anyMatch(i -> i.getSimpleName().equals("Searchable"))) {
      searchIndexer.removeIndex((Class<? extends Searchable>) c, ids);
    }
  }
}
