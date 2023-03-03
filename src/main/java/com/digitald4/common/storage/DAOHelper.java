package com.digitald4.common.storage;

import static com.google.common.collect.Streams.stream;

import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.HasModificationUser;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Annotations.DefaultDAO;
import com.digitald4.common.storage.Query.List;
import com.digitald4.common.storage.Query.Search;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.json.JSONObject;

public class DAOHelper implements DAO {
  private final DAO dao;
  private final Clock clock;
  private final Provider<User> userProvider;
  private final ChangeTracker changeTracker;
  private final SearchIndexer searchIndexer;

  @Inject
  public DAOHelper(@DefaultDAO DAO dao, Clock clock, Provider<User> userProvider,
      ChangeTracker changeTracker, SearchIndexer searchIndexer) {
    this.dao = dao;
    this.clock = clock;
    this.userProvider = userProvider;
    this.changeTracker = changeTracker;
    this.searchIndexer = searchIndexer;
  }

  @Override
  public <T> T create(T t) {
    maybeSetModificationInfo(t);

    t = dao.create(t);

    if (t instanceof ChangeTrackable) {
      changeTracker.trackCreated((ChangeTrackable<?>) t);
    }

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
      stream(entities).forEach(this::maybeSetModificationInfo);
    }

    ImmutableList<T> created = dao.create(entities);

    if (created.get(0) instanceof ChangeTrackable) {
      changeTracker.trackCreated((ImmutableList<? extends ChangeTrackable>) created);
    }

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
    T updated = dao.update(c, id, new TrackingUpdater<>(c, updater));

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

    ImmutableList<T> updated = dao.update(c, ids, new TrackingUpdater<T>(c, updater));

    if (updated.get(0) instanceof Searchable) {
      searchIndexer.index((ImmutableList<? extends Searchable>) updated);
    }

    return updated;
  }

  @Override
  public <T, I> void delete(Class<T> c, I id) {
    T defaultInstance = JSONUtil.getDefaultInstance(c);

    if (defaultInstance instanceof ChangeTrackable) {
      changeTracker.trackDeleted((ChangeTrackable<?>) get(c, id));
    }

    dao.delete(c, id);

    if (defaultInstance instanceof Searchable) {
      searchIndexer.removeIndex((Class<? extends Searchable>) c, ImmutableList.of(id));
    }
  }

  @Override
  public <T, I> void delete(Class<T> c, Iterable<I> ids) {
    T defaultInstance = JSONUtil.getDefaultInstance(c);

    if (defaultInstance instanceof ChangeTrackable) {
      changeTracker.trackDeleted((ChangeTrackable<?>) get(c, ids));
    }

    dao.delete(c, ids);

    if (defaultInstance instanceof Searchable) {
      searchIndexer.removeIndex((Class<? extends Searchable>) c, ids);
    }
  }

  private <T> void maybeSetModificationInfo(T t) {
    if (t instanceof HasModificationTimes) {
      DateTime now = new DateTime(clock.millis());
      HasModificationTimes hasModificationTimes = (HasModificationTimes) t;
      if (hasModificationTimes.getCreationTime() == null) {
        hasModificationTimes.setCreationTime(now);
      }
      hasModificationTimes.setLastModifiedTime(now);
    }

    if (t instanceof HasModificationUser) {
      User user = userProvider.get();
      HasModificationUser hasModificationUser = (HasModificationUser) t;
      if (hasModificationUser.getCreationUserId() == null) {
        hasModificationUser.setCreationUserId(user.getId());
      }
      hasModificationUser.setLastModifiedUserId(user.getId());
    }
  }

  private class TrackingUpdater<T> implements UnaryOperator<T> {
    private final UnaryOperator<T> updater;
    private final boolean hasModificationTimes;
    private final boolean hasModificationUser;
    private final boolean isChangeTrackable;

    TrackingUpdater(Class<T> c, UnaryOperator<T> updater) {
      this.updater = updater;
      T defaultInstance = JSONUtil.getDefaultInstance(c);
      hasModificationUser = defaultInstance instanceof HasModificationUser;
      hasModificationTimes = defaultInstance instanceof HasModificationTimes;
      isChangeTrackable = defaultInstance instanceof ChangeTrackable;
    }

    @Override
    public T apply(T current) {
      JSONObject jsonCurrent = isChangeTrackable ? JSONUtil.toJSON(current) : null;

      T updated = updater.apply(current);

      if (hasModificationTimes) {
        ((HasModificationTimes) updated).setLastModifiedTime(new DateTime(clock.millis()));
      }

      if (hasModificationUser) {
        ((HasModificationUser) updated).setLastModifiedUserId(userProvider.get().getId());
      }

      if (isChangeTrackable) {
        changeTracker.trackUpdated((ChangeTrackable<?>) updated, jsonCurrent);
      }

      return updated;
    }
  }
}
