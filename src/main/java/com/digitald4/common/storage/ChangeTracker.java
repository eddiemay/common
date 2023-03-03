package com.digitald4.common.storage;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Annotations.DefaultDAO;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory.ChangeType;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.json.JSONObject;

public class ChangeTracker {
  private final DAO dao;
  private final Provider<User> userProvider;
  private final Clock clock;

  @Inject
  public ChangeTracker(@DefaultDAO DAO dao, Provider<User> userProvider, Clock clock) {
    this.dao = dao;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  public <T extends ChangeTrackable<?>> ChangeHistory trackCreated(T created) {
    return trackCreated(ImmutableList.of(created)).get(0);
  }

  public <T extends ChangeTrackable<?>> ImmutableList<ChangeHistory> trackCreated(
      Iterable<T> created) {
    return dao.create(
        stream(created)
            .map(item -> createChangeHistory(ChangeType.CREATED, item))
            .collect(toImmutableList()));
  }

  public <T extends ChangeTrackable<?>> ChangeHistory trackUpdated(T updated, JSONObject original) {
    JSONObject updatedJson = JSONUtil.toJSON(updated);
    ImmutableSet<String> allFields = ImmutableSet.<String>builder()
        .addAll(original.keySet())
        .addAll(updatedJson.keySet())
        .build();

    return dao.create(createChangeHistory(ChangeType.UPDATED, updated).setChanges(
        allFields.stream()
            .filter(field -> !Objects.equals(original.opt(field), updatedJson.opt(field)))
            .map(field -> Change.create(field, original.opt(field)))
            .collect(toImmutableList())));
  }

  public <T extends ChangeTrackable<?>> ChangeHistory trackDeleted(T deleted) {
    return trackDeleted(ImmutableList.of(deleted)).get(0);
  }

  public <T extends ChangeTrackable<?>> ImmutableList<ChangeHistory> trackDeleted(
      Iterable<T> deleted) {
    return dao.create(
        stream(deleted)
            .map(item -> createChangeHistory(ChangeType.DELETED, item))
            .collect(toImmutableList()));
  }

  private <T extends ChangeTrackable<?>> ChangeHistory createChangeHistory(
      ChangeType changeType, T entity) {
    User user = userProvider.get();
    return new ChangeHistory()
        .setEntityType(entity.getClass().getSimpleName())
        .setEntityId(entity.getId())
        .setChangeType(changeType)
        .setTimeStamp(new DateTime(clock.millis()))
        .setUserId(user.getId())
        .setUsername(user.getUsername())
        .setEntity(entity);
  }

  public static class ChangeHistory extends ModelObject<Long> {
    private String entityType;
    private Object entityId;
    enum ChangeType {CREATED, UPDATED, DELETED}
    private ChangeType changeType;
    private DateTime timeStamp;
    private long userId;
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

    public Object getEntityId() {
      return entityId;
    }

    public ChangeHistory setEntityId(Object entityId) {
      this.entityId = entityId;
      return this;
    }

    public ChangeType getChangeType() {
      return changeType;
    }

    public ChangeHistory setChangeType(ChangeType changeType) {
      this.changeType = changeType;
      return this;
    }

    public DateTime getTimeStamp() {
      return timeStamp;
    }

    public ChangeHistory setTimeStamp(DateTime timeStamp) {
      this.timeStamp = timeStamp;
      return this;
    }

    public long getUserId() {
      return userId;
    }

    public ChangeHistory setUserId(long userId) {
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
