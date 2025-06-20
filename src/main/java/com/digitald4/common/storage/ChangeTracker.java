package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.HasModificationUser;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.model.SoftDeletable;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.ChangeTracker.Change.Type;
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.storage.Transaction.Op.Action;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Provider;
import org.json.JSONObject;

public class ChangeTracker {
  private final Provider<User> userProvider;
  private final UserStore<? extends User> userStore;
  private final SearchIndexer searchIndexer;
  private final Clock clock;

  @Inject
  public ChangeTracker(Provider<User> userProvider, UserStore<? extends User> userStore,
      SearchIndexer searchIndexer, Clock clock) {
    this.userProvider = userProvider;
    this.userStore = userStore;
    this.searchIndexer = searchIndexer;
    this.clock = clock;
  }

  public <T> Transaction<T> prePersist(DAO dao, Transaction<T> transaction) {
    if (transaction.getOps().isEmpty()) {
      return transaction;
    }

    ImmutableList<T> entities = transaction.executeUpdate(dao).prePersist().getOps().stream()
        .map(Op::getEntity).collect(toImmutableList());
    T first = entities.get(0);

    if (first instanceof HasModificationTimes) {
      Instant now = Instant.ofEpochMilli(clock.millis());
      entities.stream()
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
      CachedReader cachedReader = new CachedReader(dao);
      entities.stream()
          .map(t -> (HasModificationUser) t)
          .forEach(hmu -> {
            if (hmu.getCreationUserId() != null && hmu.getCreationUsername() == null) {
              hmu.setCreationUsername(userStore.get(hmu.getCreationUserId()).getUsername());
              hmu.setCreationUserId(null);
            }
            if (hmu.getCreationUsername() == null) {
              hmu.setCreationUsername(user.getUsername());
            }

            if (hmu.getLastModifiedUserId() != null && hmu.getLastModifiedUsername() == null) {
              hmu.setLastModifiedUsername(userStore.get(hmu.getLastModifiedUserId()).getUsername());
              hmu.setLastModifiedUserId(null);
            } else {
              hmu.setLastModifiedUsername(user.getUsername());
            }

            if (hmu instanceof SoftDeletable<?> softDelete) {
              if (softDelete.getDeletionTime() != null && softDelete.getDeletionUsername() == null) {
                softDelete.setDeletionUsername(user.getUsername());
              }
            }
          });
    }

    return transaction;
  }

  public <T> Transaction<T> postPersist(DAO dao, Transaction<T> transaction) {
    if (transaction.getOps().isEmpty()) {
      return transaction;
    }

    ImmutableList<Op<T>> ops = transaction.postPersist().getOps();
    T first = ops.get(0).getEntity();

    if (first instanceof ChangeTrackable) {
      dao.persist(
          Transaction.of(ops.stream().map(this::createChangeHistory).collect(toImmutableList())));
    }

    if (first instanceof Searchable) {
      searchIndexer.index((Iterable<? extends Searchable>)
          ops.stream().map(Op::getEntity).collect(toImmutableList()));
    }

    return transaction;
  }

  public <T, I> void preDelete(DAO dao, Class<T> c, Iterable<I> ids) {
    T defaultInstance = JSONUtil.getDefaultInstance(c);

    if (defaultInstance instanceof ChangeTrackable) {
      trackDeleted(dao, (Class<? extends ChangeTrackable>) c, ids);
    }
  }

  public <T, I> void postDelete(Class<T> c, Iterable<I> ids) {
    T defaultInstance = JSONUtil.getDefaultInstance(c);

    if (defaultInstance instanceof Searchable) {
      searchIndexer.removeIndex((Class<? extends Searchable>) c, ids);
    }
  }

  public <ID, T extends ChangeTrackable<?>> ImmutableList<ChangeHistory> trackDeleted(
      DAO dao, Class<T> c, Iterable<ID> ids) {
    return dao
        .persist(Transaction.of(
            dao.get(c, ids).getItems().stream()
                .filter(item -> item instanceof ChangeTrackable)
                .map(item -> Op.deleted(item))
                .map(this::createChangeHistory)
                .collect(toImmutableList())))
        .getOps().stream().map(Op::getEntity).collect(toImmutableList());
  }

  @VisibleForTesting Op<ChangeHistory> createChangeHistory(Op<?> op) {
    User user = userProvider.get();
    return Op.create(
        new ChangeHistory()
            .setEntityType(op.getTypeClass().getSimpleName())
            .setEntityId(String.valueOf(op.getId()))
            .setAction(op.getAction())
            .setTimeStamp(clock.millis())
            .setUserId(user.getId())
            .setUsername(user.getUsername())
            .setEntity(new JSONObject(op.getEntity()).toString())
            .setChanges(commuteChanges(op)));
  }

  private static ImmutableList<Change> commuteChanges(Op<?> op) {
    if (op.getCurrent() == null) {
      return null;
    }

    return commuteChanges(new JSONObject(op.getEntity()), new JSONObject(op.getCurrent()));
  }

  public static ImmutableList<Change> commuteChanges(JSONObject curr, JSONObject prev) {
    var adds = curr.keySet().stream().filter(key -> !Objects.equals(curr.get(key), prev.opt(key)))
        .map(key -> {
          Object value = curr.get(key);
          Object prevValue = prev.opt(key);
          if (value instanceof JSONObject && prevValue instanceof JSONObject) {
            return Change.create(key, commuteChanges((JSONObject) value, (JSONObject) prevValue));
          }
          return Change.create(key, prev.has(key) ? Type.Modified : Type.Add, "" + value, prevValue);
        });
    var removes = prev.keySet().stream()
        .filter(key -> !curr.has(key)).map(key -> Change.create(key, Type.Removed, null, prev.get(key)));

    return Streams.concat(adds, removes).collect(toImmutableList());
  }

  public static class ChangeHistory extends ModelObject<Long> {
    private String entityType;
    private String entityId;
    private Action action;
    private Instant timeStamp;
    private Long userId;
    private String username;
    private StringBuilder entity;
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

    public StringBuilder getEntity() {
      return entity;
    }

    public ChangeHistory setEntity(StringBuilder entity) {
      this.entity = entity;
      return this;
    }

    public ChangeHistory setEntity(String entity) {
      return setEntity(new StringBuilder(entity));
    }

    public ImmutableList<Change> getChanges() {
      return changes;
    }

    public ChangeHistory setChanges(Iterable<Change> changes) {
      this.changes = changes == null ? null : ImmutableList.copyOf(changes);
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
