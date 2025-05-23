package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.joining;

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
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Provider;

public class ChangeTracker {
  private final Provider<DAO> daoProvider;
  private final Provider<User> userProvider;
  private final UserStore<? extends User> userStore;
  private final SearchIndexer searchIndexer;
  private final Clock clock;

  @Inject
  public ChangeTracker(Provider<DAO> daoProvider, Provider<User> userProvider, UserStore<? extends User> userStore,
      SearchIndexer searchIndexer, Clock clock) {
    this.daoProvider = daoProvider;
    this.userProvider = userProvider;
    this.userStore = userStore;
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
      CachedReader cachedReader = new CachedReader(daoProvider.get());
      stream(entities)
          .map(t -> (HasModificationUser) t)
          .forEach(hasModificationUser -> {
            if (hasModificationUser.getCreationUserId() != null && hasModificationUser.getCreationUsername() == null) {
              hasModificationUser.setCreationUsername(
                  userStore.get(hasModificationUser.getCreationUserId()).getUsername());
              hasModificationUser.setCreationUserId(null);
            }
            if (hasModificationUser.getCreationUsername() == null) {
              hasModificationUser.setCreationUsername(user.getUsername());
            }

            if (hasModificationUser.getLastModifiedUserId() != null && hasModificationUser.getLastModifiedUsername() == null) {
              hasModificationUser.setLastModifiedUsername(
                  userStore.get(hasModificationUser.getLastModifiedUserId()).getUsername());
              hasModificationUser.setLastModifiedUserId(null);
            } else {
              hasModificationUser.setLastModifiedUsername(user.getUsername());
            }
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

  public <ID, T extends ChangeTrackable<?>> ImmutableList<ChangeHistory> trackDeleted(Class<T> c, Iterable<ID> ids) {
    DAO dao = daoProvider.get();
    return dao.create(dao.get(c, ids).getItems().stream()
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
    public enum Action {CREATED, UPDATED, MIGRATED, DELETED}
    private Action action;
    private Instant timeStamp;
    private Long userId;
    private String username;
    private Object entity;
    private ImmutableList<Change> changes;

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

    @ApiResourceProperty
    public long timeStamp() {
      return timeStamp.toEpochMilli();
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Instant getTimeStamp() {
      return timeStamp;
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

    public ImmutableList<Change> getChanges() {
      return changes;
    }

    public ChangeHistory setChanges(Iterable<Change> changes) {
      this.changes = ImmutableList.copyOf(changes);
      return this;
    }

    private static final String CHANGE_TEMPLATE =
        "%s: <span class=\"diff-insert\">%s</span> <span class=\"diff-delete\">%s</span>";
    @ApiResourceProperty
    public String changeHtml() {
      return changes == null ? null : changes.stream()
          .map(c -> {
            if (c.getSubChanges() != null) {
              return String.format("%s: {%s}", c.getField(), c.getSubChanges().stream()
                  .map(sc -> String.format(CHANGE_TEMPLATE, sc.getField(), sc.getValue(), sc.getPreviousValue()))
                  .collect(joining(", ")));
            }
            return String.format(CHANGE_TEMPLATE, c.getField(), c.getValue(), c.getPreviousValue());
          })
          .collect(joining("<br>"));
    }
  }

  public static class Change {
    private String field;
    public enum Type {Add, Modified, Removed};
    private Type type;
    private String value;
    private String previousValue;
    private ImmutableList<Change> subChanges;

    public String getField() {
      return field;
    }

    public Change setField(String field) {
      this.field = field;
      return this;
    }

    public Type getType() {
      return type;
    }

    public Change setType(Type type) {
      this.type = type;
      return this;
    }

    public String getValue() {
      return value;
    }

    public Change setValue(String value) {
      this.value = value != null ? value : "";
      return this;
    }

    public String getPreviousValue() {
      return previousValue;
    }

    public Change setPreviousValue(String previousValue) {
      this.previousValue = previousValue != null ? previousValue : "";
      return this;
    }

    public ImmutableList<Change> getSubChanges() {
      return subChanges;
    }

    public Change setSubChanges(Iterable<Change> subChanges) {
      this.subChanges = ImmutableList.copyOf(subChanges);
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

    public static Change create(String field, Type type, String value, Object from) {
      return new Change().setField(field).setType(type).setValue(value).setPreviousValue(from != null ? "" + from : "");
    }

    public static Change create(String field, Iterable<Change> subChanges) {
      return new Change().setField(field).setSubChanges(subChanges);
    }
  }
}
