package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.*;
import com.digitald4.common.server.service.BulkGetable;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import javax.inject.Provider;;
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
    // when(dao.get(any(), eq(ImmutableList.of()))).thenReturn(ImmutableList.of());
    changeTracker = new ChangeTracker(() -> dao, userProvider, null, searchIndexer, clock);
  }

  @Test
  public void createPojo() {
    changeTracker.prePersist(ImmutableList.of(new Pojo()));
    changeTracker.postPersist(ImmutableList.of(new Pojo()), true);

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deletePojo() {
    changeTracker.preDelete(Pojo.class, ImmutableList.of(75L));
    changeTracker.postDelete(Pojo.class, ImmutableList.of(75L));

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModTimes() {
    ModelObjectModTime modTimes = new ModelObjectModTime();
    changeTracker.prePersist(ImmutableList.of(modTimes));
    changeTracker.postPersist(ImmutableList.of(modTimes), true);

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modTimes.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void updateModTimes() {
    HasModificationTimes modTimes = new ModelObjectModTime().setCreationTime(500L).setLastModifiedTime(500L);

    changeTracker.prePersist(ImmutableList.of(modTimes));
    changeTracker.postPersist(ImmutableList.of(modTimes), false);

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().toEpochMilli()).isEqualTo(500L);
    assertThat(modTimes.getLastModifiedTime().toEpochMilli()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void deleteModTimes() {
    changeTracker.preDelete(ModelObjectModTime.class, ImmutableList.of(75L));
    changeTracker.postDelete(ModelObjectModTime.class, ImmutableList.of(75L));

    // Modtimes actually does not support deletiontime
    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModUser() {
    ModelObjectModUser modUser = new ModelObjectModUser();

    changeTracker.prePersist(ImmutableList.of(modUser));
    changeTracker.postPersist(ImmutableList.of(modUser), true);

    verify(clock).millis();
    verify(dao, never()).create(anyList());
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

    changeTracker.prePersist(ImmutableList.of(modUser));
    changeTracker.postPersist(ImmutableList.of(modUser), false);

    verify(clock).millis();
    verify(dao, never()).create(anyList());
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
    changeTracker.preDelete(ModelObjectModUser.class, ImmutableList.of(75L));
    changeTracker.postDelete(ModelObjectModUser.class, ImmutableList.of(75L));

    // Moduser actually does not support deletion.
    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createTrackable() {
    Trackable trackable = new Trackable();

    changeTracker.prePersist(ImmutableList.of(trackable));
    changeTracker.postPersist(ImmutableList.of(trackable), true);

    verify(clock).millis();
    verify(dao).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void updateTrackable() {
    Trackable trackable = new Trackable().setId(75L);
    // when(dao.get(Trackable.class, ImmutableList.of(75L))).thenReturn(ImmutableList.of(trackable));

    changeTracker.prePersist(ImmutableList.of(trackable));
    changeTracker.postPersist(ImmutableList.of(trackable), false);

    verify(clock).millis();
    verify(dao).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deleteTrackable() {
    Trackable trackable = new Trackable();
    when(dao.get(Trackable.class, ImmutableList.of(75L))).thenReturn(
        BulkGetable.MultiListResult.of(ImmutableList.of(trackable), ImmutableList.of(75L)));

    changeTracker.preDelete(Trackable.class, ImmutableList.of(75L));
    changeTracker.postDelete(Trackable.class, ImmutableList.of(75L));

    verify(clock).millis();
    verify(dao).create(anyList());
    verify(searchIndexer, never()).removeIndex(any(), any());
  }

  @Test
  public void createSearchable() {
    SearchableObj searchable = new SearchableObj();

    changeTracker.prePersist(ImmutableList.of(searchable));
    changeTracker.postPersist(ImmutableList.of(searchable), true);

    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer).index(ImmutableList.of(searchable));
  }

  @Test
  public void updateSearchable() {
    SearchableObj searchableObj = new SearchableObj();

    changeTracker.prePersist(ImmutableList.of(searchableObj));
    changeTracker.postPersist(ImmutableList.of(searchableObj), false);

    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer).index(ImmutableList.of(searchableObj));
  }

  @Test
  public void deleteSearchable() {
    changeTracker.preDelete(SearchableObj.class, ImmutableList.of(75L));
    changeTracker.postDelete(SearchableObj.class, ImmutableList.of(75L));

    verify(clock, never()).millis();
    verify(dao, never()).create(anyList());
    verify(searchIndexer).removeIndex(SearchableObj.class, ImmutableList.of(75L));
  }

  @Test
  public void createSubClassAll() {
    SubAll subAll = new SubAll();

    changeTracker.prePersist(ImmutableList.of(subAll));
    changeTracker.postPersist(ImmutableList.of(subAll), true);

    verify(clock, times(2)).millis();
    verify(dao).create(anyList());
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

    changeTracker.prePersist(ImmutableList.of(subAll));
    changeTracker.postPersist(ImmutableList.of(subAll), false);

    verify(clock, times(2)).millis();
    verify(dao).create(anyList());
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

    changeTracker.preDelete(SubAll.class, ImmutableList.of(75L));
    changeTracker.postDelete(SubAll.class, ImmutableList.of(75L));

    verify(clock).millis();
    verify(dao).create(anyList());
    verify(searchIndexer).removeIndex(SubAll.class, ImmutableList.of(75L));
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
