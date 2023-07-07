package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static java.util.function.Function.identity;

import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.HasModificationUser;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory.Action;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Provider;
import org.json.JSONObject;

public class ChangeTracker {
  private final Provider<DAO> daoProvider;
  private final Provider<User> userProvider;
  private final SearchIndexer searchIndexer;
  private final Clock clock;

  @Inject
  public ChangeTracker(Provider<DAO> daoProvider, Provider<User> userProvider,
      SearchIndexer searchIndexer, Clock clock) {
    this.daoProvider = daoProvider;
    this.userProvider = userProvider;
    this.searchIndexer = searchIndexer;
    this.clock = clock;
  }

  public <T> Iterable<T> prePersist(Iterable<T> entities) {
    if (!entities.iterator().hasNext()) {
      return entities;
    }

    T first = entities.iterator().next();

    if (first instanceof HasModificationTimes) {
      Instant now = Instant.ofEpochMilli(clock.millis());
      stream(entities)
          .map(t -> (HasModificationTimes) t)
          .forEach(hasModificationTimes -> {
            if (hasModificationTimes.getCreationTime() == null) {
              hasModificationTimes.setCreationTime(now);
            }
            hasModificationTimes.setLastModifiedTime(now);
          });
    }

    if (first instanceof HasModificationUser) {
      User user = userProvider.get();
      stream(entities)
          .map(t -> (HasModificationUser) t)
          .forEach(hasModificationUser -> {
            if (hasModificationUser.getCreationUserId() == null) {
              hasModificationUser.setCreationUserId(user.getId());
            }
            hasModificationUser.setLastModifiedUserId(user.getId());
          });
    }

    return entities;
  }

  public <T> ImmutableList<T> postPersist(ImmutableList<T> entities, boolean isCreate) {
    if (entities.isEmpty()) {
      return entities;
    }

    T first = entities.get(0);

    if (first instanceof ChangeTrackable) {
      trackRevised((Iterable<? extends ChangeTrackable>) entities, isCreate);
    }

    if (first instanceof Searchable) {
      searchIndexer.index((Iterable<? extends Searchable>) entities);
    }

    return entities;
  }

  public <T, I> void preDelete(Class<T> c, Iterable<I> ids) {
    T defaultInstance = JSONUtil.getDefaultInstance(c);

    if (defaultInstance instanceof ChangeTrackable) {
      trackDeleted((Class<? extends ChangeTrackable>) c, ids);
    }
  }

  public <T, I> void postDelete(Class<T> c, Iterable<I> ids) {
    T defaultInstance = JSONUtil.getDefaultInstance(c);

    if (defaultInstance instanceof Searchable) {
      searchIndexer.removeIndex((Class<? extends Searchable>) c, ids);
    }
  }

  @VisibleForTesting
  <T extends ChangeTrackable<?>> ImmutableList<ChangeHistory> trackRevised(
      Iterable<T> items, boolean isCreate) {
    return daoProvider.get().create(
        stream(items)
            .map(item -> createChangeHistory(isCreate ? Action.CREATED : Action.UPDATED, item))
            .collect(toImmutableList()));
  }

  public <ID, T extends ChangeTrackable<?>> ImmutableList<ChangeHistory> trackDeleted(
      Class<T> c, Iterable<ID> ids) {
    DAO dao = daoProvider.get();
    return dao.create(dao.get(c, ids).stream()
        .map(item -> createChangeHistory(Action.DELETED, item))
        .collect(toImmutableList()));
  }

  private <T extends ChangeTrackable<?>> ChangeHistory createChangeHistory(Action action, T entity) {
    User user = userProvider.get();
    return new ChangeHistory()
        .setEntityType(entity.getClass().getSimpleName())
        .setEntityId(String.valueOf(entity.getId()))
        .setAction(action)
        .setTimeStamp(clock.millis())
        .setUserId(user.getId())
        .setUsername(user.getUsername())
        .setEntity(entity);
  }

  public static class ChangeHistory extends ModelObject<Long> {
    private String entityType;
    private String entityId;
    enum Action {CREATED, UPDATED, DELETED}
    private Action action;
    private Instant timeStamp;
    private Long userId;
    private String username;
    private Object entity;

    public ChangeHistory setId(Long id) {
      super.setId(id);
      return this;
    }

    public String getEntityType() {
      return entityType;
    }

    public ChangeHistory setEntityType(String entityType) {
      this.entityType = entityType;
      return this;
    }

    public String getEntityId() {
      return entityId;
    }

    public ChangeHistory setEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Action getAction() {
      return action;
    }

    public ChangeHistory setAction(Action action) {
      this.action = action;
      return this;
    }

    @Deprecated
    public Action getChangeType() {
      return null;
    }

    @Deprecated
    public ChangeHistory setChangeType(Action action) {
      this.action = action;
      return this;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Instant getTimeStamp() {
      return timeStamp;
    }

    @ApiResourceProperty
    public long timeStamp() {
      return timeStamp.toEpochMilli();
    }

    public ChangeHistory setTimeStamp(long millis) {
      this.timeStamp = Instant.ofEpochMilli(millis);
      return this;
    }

    public Long getUserId() {
      return userId;
    }

    public ChangeHistory setUserId(Long userId) {
      this.userId = userId;
      return this;
    }

    public String getUsername() {
      return username;
    }

    public ChangeHistory setUsername(String username) {
      this.username = username;
      return this;
    }

    public Object getEntity() {
      return entity;
    }

    public ChangeHistory setEntity(Object entity) {
      this.entity = entity;
      return this;
    }

    @Deprecated
    public ImmutableList<Change> getChanges() {
      return null;
    }

    @Deprecated
    public ChangeHistory setChanges(Iterable<Change> changes) {
      return this;
    }
  }

  public static class Change {
    private String field;
    private Object previousValue;

    public String getField() {
      return field;
    }

    public Change setField(String field) {
      this.field = field;
      return this;
    }

    public Object getPreviousValue() {
      return previousValue;
    }

    public Change setPreviousValue(Object previousValue) {
      this.previousValue = previousValue;
      return this;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Change && Objects.equals(toString(), obj.toString());
    }

    @Override
    public String toString() {
      return JSONUtil.toJSON(this).toString();
    }

    public static Change create(String field, Object from) {
      return new Change().setField(field).setPreviousValue(from);
    }
  }
}
