package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.HasModificationUser;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.model.User;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

public class ChangeProcessorTest {
  private final User user = new BasicUser().setId(1001L).setUsername("username");
  private final DAO dao = mock(DAO.class);
  private final Provider<User> userProvider = mock(Provider.class);
  private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  private final Clock clock = mock(Clock.class);
  private ChangeTracker changeTracker;

  @Before
  public void setup() {
    when(clock.millis()).thenReturn(1000L);
    when(userProvider.get()).thenReturn(user);
    when(dao.get(any(), eq(ImmutableList.of()))).thenReturn(ImmutableList.of());
    changeTracker = new ChangeTracker(() -> dao, userProvider, searchIndexer, clock);
  }

  @Test
  public void createPojo() {
    changeTracker.prePersist(ImmutableList.of(new Pojo()));
    changeTracker.postPersist(ImmutableList.of(new Pojo()), true);

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deletePojo() {
    changeTracker.preDelete(Pojo.class, ImmutableList.of(75L));
    changeTracker.postDelete(Pojo.class, ImmutableList.of(75L));

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModTimes() {
    ModTimes modTimes = new ModTimes();
    changeTracker.prePersist(ImmutableList.of(modTimes));
    changeTracker.postPersist(ImmutableList.of(modTimes), true);

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().getMillis()).isEqualTo(1000L);
    assertThat(modTimes.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void updateModTimes() {
    ModTimes modTimes =
        new ModTimes().setCreationTime(new DateTime(500L)).setLastModifiedTime(new DateTime(500L));

    changeTracker.prePersist(ImmutableList.of(modTimes));
    changeTracker.postPersist(ImmutableList.of(modTimes), false);

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().getMillis()).isEqualTo(500L);
    assertThat(modTimes.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void deleteModTimes() {
    changeTracker.preDelete(ModTimes.class, ImmutableList.of(75L));
    changeTracker.postDelete(ModTimes.class, ImmutableList.of(75L));

    // Modtimes actually does not support deletiontime
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModUser() {
    ModUser modUser = new ModUser();

    changeTracker.prePersist(ImmutableList.of(modUser));
    changeTracker.postPersist(ImmutableList.of(modUser), true);

    // ModUser uses clock and userProvider.
    verify(clock).millis();
    verify(userProvider).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());

    assertThat(modUser.getCreationTime().getMillis()).isEqualTo(1000L);
    assertThat(modUser.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(modUser.getDeletionTime()).isNull();

    assertThat(modUser.getCreationUserId()).isEqualTo(1001L);
    assertThat(modUser.getLastModifiedUserId()).isEqualTo(1001L);
    assertThat(modUser.getDeletionUserId()).isNull();
  }

  @Test
  public void updateModUser() {
    ModUser modUser =
        (ModUser) new ModUser().setCreationUserId(501L).setCreationTime(new DateTime(500L));

    changeTracker.prePersist(ImmutableList.of(modUser));
    changeTracker.postPersist(ImmutableList.of(modUser), false);

    // Moduser uses clock and userProvider.
    verify(clock).millis();
    verify(userProvider).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());

    assertThat(modUser.getCreationTime().getMillis()).isEqualTo(500L);
    assertThat(modUser.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(modUser.getDeletionTime()).isNull();

    assertThat(modUser.getCreationUserId()).isEqualTo(501L);
    assertThat(modUser.getLastModifiedUserId()).isEqualTo(1001L);
    assertThat(modUser.getDeletionUserId()).isNull();
  }

  @Test
  public void deleteModUser() {
    changeTracker.preDelete(ModUser.class, ImmutableList.of(75L));
    changeTracker.postDelete(ModUser.class, ImmutableList.of(75L));

    // Moduser actually does not support deletion.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createTrackable() {
    Trackable trackable = new Trackable();

    changeTracker.prePersist(ImmutableList.of(trackable));
    changeTracker.postPersist(ImmutableList.of(trackable), true);

    verify(clock).millis();
    verify(userProvider).get();
    verify(dao).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void updateTrackable() {
    Trackable trackable = new Trackable().setId(75L);
    when(dao.get(Trackable.class, ImmutableList.of(75L))).thenReturn(ImmutableList.of(trackable));

    changeTracker.prePersist(ImmutableList.of(trackable));
    changeTracker.postPersist(ImmutableList.of(trackable), false);

    verify(clock).millis();
    verify(userProvider).get();
    verify(dao).create(anyList());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deleteTrackable() {
    Trackable trackable = new Trackable();
    when(dao.get(Trackable.class, ImmutableList.of(75L))).thenReturn(ImmutableList.of(trackable));

    changeTracker.preDelete(Trackable.class, ImmutableList.of(75L));
    changeTracker.postDelete(Trackable.class, ImmutableList.of(75L));

    verify(clock).millis();
    verify(userProvider).get();
    verify(dao).create(anyList());
    verify(searchIndexer, never()).removeIndex(any(), any());
  }

  @Test
  public void createSearchable() {
    SearchableObj searchable = new SearchableObj();

    changeTracker.prePersist(ImmutableList.of(searchable));
    changeTracker.postPersist(ImmutableList.of(searchable), true);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer).index(ImmutableList.of(searchable));
  }

  @Test
  public void updateSearchable() {
    SearchableObj searchableObj = new SearchableObj();

    changeTracker.prePersist(ImmutableList.of(searchableObj));
    changeTracker.postPersist(ImmutableList.of(searchableObj), false);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer).index(ImmutableList.of(searchableObj));
  }

  @Test
  public void deleteSearchable() {
    changeTracker.preDelete(SearchableObj.class, ImmutableList.of(75L));
    changeTracker.postDelete(SearchableObj.class, ImmutableList.of(75L));

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(dao, never()).create(anyList());
    verify(searchIndexer).removeIndex(SearchableObj.class, ImmutableList.of(75L));
  }

  @Test
  public void createSubClassAll() {
    SubAll subAll = new SubAll();

    changeTracker.prePersist(ImmutableList.of(subAll));
    changeTracker.postPersist(ImmutableList.of(subAll), true);

    verify(clock, times(2)).millis();
    verify(userProvider, times(2)).get();
    verify(dao).create(anyList());
    verify(searchIndexer).index(ImmutableList.of(subAll));

    assertThat(subAll.getCreationTime().getMillis()).isEqualTo(1000L);
    assertThat(subAll.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(subAll.getDeletionTime()).isNull();

    assertThat(subAll.getCreationUserId()).isEqualTo(1001L);
    assertThat(subAll.getLastModifiedUserId()).isEqualTo(1001L);
    assertThat(subAll.getDeletionUserId()).isNull();
  }

  @Test
  public void updateSubAll() {
    SubAll subAll = (SubAll)
        new SubAll().setId(75L).setCreationUserId(501L).setCreationTime(new DateTime(500L));

    when(dao.get(SubAll.class, ImmutableList.of(75L))).thenReturn(ImmutableList.of(subAll));

    changeTracker.prePersist(ImmutableList.of(subAll));
    changeTracker.postPersist(ImmutableList.of(subAll), false);

    verify(clock, times(2)).millis();
    verify(userProvider, times(2)).get();
    verify(dao).create(anyList());
    verify(searchIndexer).index(ImmutableList.of(subAll));

    assertThat(subAll.getCreationTime().getMillis()).isEqualTo(500L);
    assertThat(subAll.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(subAll.getDeletionTime()).isNull();

    assertThat(subAll.getCreationUserId()).isEqualTo(501L);
    assertThat(subAll.getLastModifiedUserId()).isEqualTo(1001L);
    assertThat(subAll.getDeletionUserId()).isNull();
  }

  @Test
  public void deleteSubAll() {
    SubAll subAll = (SubAll) new SubAll().setId(75L);
    when(dao.get(SubAll.class, ImmutableList.of(75L))).thenReturn(ImmutableList.of(subAll));

    changeTracker.preDelete(SubAll.class, ImmutableList.of(75L));
    changeTracker.postDelete(SubAll.class, ImmutableList.of(75L));

    verify(clock).millis();
    verify(userProvider).get();
    verify(dao).create(anyList());
    verify(searchIndexer).removeIndex(SubAll.class, ImmutableList.of(75L));
  }

  public static class Pojo {}

  public static class ModTimes extends ModelObject<Long> implements HasModificationTimes {
    private DateTime creationTime;
    private DateTime lastModifiedTime;
    private DateTime deletedTime;

    @Override
    public DateTime getCreationTime() {
      return creationTime;
    }

    @Override
    public ModTimes setCreationTime(DateTime time) {
      this.creationTime = time;
      return this;
    }

    @Override
    public DateTime getLastModifiedTime() {
      return lastModifiedTime;
    }

    @Override
    public ModTimes setLastModifiedTime(DateTime time) {
      this.lastModifiedTime = time;
      return this;
    }

    @Override
    public DateTime getDeletionTime() {
      return deletedTime;
    }

    @Override
    public ModTimes setDeletionTime(DateTime time) {
      this.deletedTime = time;
      return this;
    }
  }

  public static class ModUser extends ModTimes implements HasModificationUser {
    private Long creationUserId;
    private Long lastModificationUserId;
    private Long deletedUserId;

    @Override
    public Long getCreationUserId() {
      return creationUserId;
    }

    @Override
    public ModUser setCreationUserId(Long userId) {
      this.creationUserId = userId;
      return this;
    }

    @Override
    public Long getLastModifiedUserId() {
      return lastModificationUserId;
    }

    @Override
    public ModUser setLastModifiedUserId(Long userId) {
      this.lastModificationUserId = userId;
      return this;
    }

    @Override
    public Long getDeletionUserId() {
      return deletedUserId;
    }

    @Override
    public ModUser setDeletionUserId(Long userId) {
      this.deletedUserId = userId;
      return this;
    }
  }

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

  public static class ImplAll extends ModUser implements ChangeTrackable<Long>, Searchable {
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
