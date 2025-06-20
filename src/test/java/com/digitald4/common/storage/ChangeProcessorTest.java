package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.*;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Transaction.Op;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import javax.inject.Provider;
import com.google.inject.util.Providers;
import org.junit.Before;
import org.junit.Test;

public class ChangeProcessorTest {
  private final User user = new BasicUser().setId(1001L).setUsername("username");
  private final DAO dao = mock(DAO.class);
  private final Provider<User> userProvider = Providers.of(user);
  private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  private final Clock clock = mock(Clock.class);
  private ChangeTracker changeTracker;

  @Before
  public void setup() {
    when(clock.millis()).thenReturn(1000L);
    when(dao.get(any(), eq(ImmutableList.of()))).thenReturn(
        BulkGetable.MultiListResult.of(ImmutableList.of(), ImmutableList.of()));
    when(dao.persist(any())).then(i -> i.getArgument(0));
    changeTracker = new ChangeTracker(userProvider, null, searchIndexer, clock);
  }

  @Test
  public void createPojo() {
    var op = Op.create(new Pojo());
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deletePojo() {
    changeTracker.preDelete(dao, Pojo.class, ImmutableList.of(75L));
    changeTracker.postDelete(Pojo.class, ImmutableList.of(75L));

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModTimes() {
    ModelObjectModTime modTimes = new ModelObjectModTime();
    var op = Op.create(modTimes);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modTimes.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void updateModTimes() {
    HasModificationTimes modTimes = new ModelObjectModTime().setCreationTime(500L).setLastModifiedTime(500L);

    var op = Op.update(HasModificationTimes.class, 500L, null).setEntity(modTimes);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().toEpochMilli()).isEqualTo(500L);
    assertThat(modTimes.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
  }

  @Test
  public void deleteModTimes() {
    changeTracker.preDelete(dao, ModelObjectModTime.class, ImmutableList.of(75L));
    changeTracker.postDelete(ModelObjectModTime.class, ImmutableList.of(75L));

    // Modtimes actually does not support deletiontime
    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModUser() {
    ModelObjectModUser modUser = new ModelObjectModUser();

    var op = Op.create(modUser);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());

    assertThat(modUser.getCreationTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modUser.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modUser.getDeletionTime()).isNull();

    assertThat(modUser.getCreationUsername()).isEqualTo("username");
    assertThat(modUser.getLastModifiedUsername()).isEqualTo("username");
    assertThat(modUser.getDeletionUsername()).isNull();
  }

  @Test
  public void updateModUser() {
    ModelObjectModUser modUser = (ModelObjectModUser)
        new ModelObjectModUser().setCreationUsername("username").setCreationTime(Instant.ofEpochMilli(500L));

    var op = createUpdated(modUser);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());

    assertThat(modUser.getCreationTime().toEpochMilli()).isEqualTo(500L);
    assertThat(modUser.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modUser.getDeletionTime()).isNull();

    assertThat(modUser.getCreationUsername()).isEqualTo("username");
    assertThat(modUser.getLastModifiedUsername()).isEqualTo("username");
    assertThat(modUser.getDeletionUsername()).isNull();
  }

  @Test
  public void deleteModUser() {
    changeTracker.preDelete(dao, ModelObjectModUser.class, ImmutableList.of(75L));
    changeTracker.postDelete(ModelObjectModUser.class, ImmutableList.of(75L));

    // Moduser actually does not support deletion.
    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createTrackable() {
    Trackable trackable = new Trackable();

    var op = Op.create(trackable);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock).millis();
    verify(dao).persist(any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void updateTrackable() {
    Trackable trackable = new Trackable().setId(75L);

    var op = createUpdated(trackable);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock).millis();
    verify(dao).persist(any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deleteTrackable() {
    Trackable trackable = new Trackable();
    when(dao.get(Trackable.class, ImmutableList.of(75L))).thenReturn(
        BulkGetable.MultiListResult.of(ImmutableList.of(trackable), ImmutableList.of(75L)));

    changeTracker.preDelete(dao, Trackable.class, ImmutableList.of(75L));
    changeTracker.postDelete(Trackable.class, ImmutableList.of(75L));

    verify(clock).millis();
    verify(dao).persist(any());
    verify(searchIndexer, never()).removeIndex(any(), any());
  }

  @Test
  public void createSearchable() {
    SearchableObj searchable = new SearchableObj();

    var op = Op.create(searchable);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer).index(ImmutableList.of(searchable));
  }

  @Test
  public void updateSearchable() {
    SearchableObj searchableObj = new SearchableObj();

    var op = createUpdated(searchableObj);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer).index(ImmutableList.of(searchableObj));
  }

  @Test
  public void deleteSearchable() {
    changeTracker.preDelete(dao, SearchableObj.class, ImmutableList.of(75L));
    changeTracker.postDelete(SearchableObj.class, ImmutableList.of(75L));

    verify(clock, never()).millis();
    verify(dao, never()).persist(any());
    verify(searchIndexer).removeIndex(SearchableObj.class, ImmutableList.of(75L));
  }

  @Test
  public void createSubClassAll() {
    SubAll subAll = new SubAll();

    var op = Op.create(subAll);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock, times(2)).millis();
    verify(dao).persist(any());
    verify(searchIndexer).index(ImmutableList.of(subAll));

    assertThat(subAll.getCreationTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(subAll.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(subAll.getDeletionTime()).isNull();

    assertThat(subAll.getCreationUsername()).isEqualTo("username");
    assertThat(subAll.getLastModifiedUsername()).isEqualTo("username");
    assertThat(subAll.getDeletionUsername()).isNull();
  }

  @Test
  public void updateSubAll() {
    SubAll subAll =
        (SubAll) new SubAll().setId(75L).setCreationUsername("username").setCreationTime(Instant.ofEpochMilli(500L));

    // when(dao.get(SubAll.class, ImmutableList.of(75L))).thenReturn(ImmutableList.of(subAll));

    var op = createUpdated(subAll);
    changeTracker.prePersist(dao, Transaction.of(op));
    changeTracker.postPersist(dao, Transaction.of(op));

    verify(clock, times(2)).millis();
    verify(dao).persist(any());
    verify(searchIndexer).index(ImmutableList.of(subAll));

    assertThat(subAll.getCreationTime().toEpochMilli()).isEqualTo(500L);
    assertThat(subAll.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(subAll.getDeletionTime()).isNull();

    assertThat(subAll.getCreationUsername()).isEqualTo("username");
    assertThat(subAll.getLastModifiedUsername()).isEqualTo("username");
    assertThat(subAll.getDeletionUsername()).isNull();
  }

  @Test
  public void deleteSubAll() {
    SubAll subAll = (SubAll) new SubAll().setId(75L);
    when(dao.get(SubAll.class, ImmutableList.of(75L))).thenReturn(
        BulkGetable.MultiListResult.of(ImmutableList.of(subAll), ImmutableList.of(75L)));

    changeTracker.preDelete(dao, SubAll.class, ImmutableList.of(75L));
    changeTracker.postDelete(SubAll.class, ImmutableList.of(75L));

    verify(clock).millis();
    verify(dao).persist(any());
    verify(searchIndexer).removeIndex(SubAll.class, ImmutableList.of(75L));
  }

  private static <T extends ModelObject<?>> Op<T> createUpdated(T t) {
    return Op.update((Class<T>) t.getClass(), t.getId(), null).setEntity(t);
  }

  public static class Pojo {}

  public static class Trackable extends ModelObject<Long> implements ChangeTrackable<Long> {
    private Long id;

    @Override
    public Long getId() {
      return id;
    }

    public Trackable setId(Long id) {
      this.id = id;
      return this;
    }
  }
  
  public static class SearchableObj extends ModelObject<Long> implements Searchable {}

  public static class ImplAll extends ModelObjectModUser implements ChangeTrackable<Long>, Searchable {
    private Long id;

    public ImplAll setId(Long id) {
      this.id = id;
      return this;
    }

    @Override
    public Long getId() {
      return id;
    }
  }

  public static class SubAll extends ImplAll {}
}
