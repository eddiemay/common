package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.function.UnaryOperator.identity;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.HasModificationUser;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.model.User;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.function.UnaryOperator;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

public class DAOHelperTest {
  private final DAO dao = mock(DAO.class);
  private final User user = new BasicUser().setId(1001L).setUsername("username");
  private final Provider<User> userProvider = mock(Provider.class);
  private final Clock clock = mock(Clock.class);
  private final ChangeTracker changeTracker = mock(ChangeTracker.class);
  private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  private DAOHelper daoHelper;

  @Before
  public void setup() {
    when(clock.millis()).thenReturn(1000L);
    when(userProvider.get()).thenReturn(user);
    when(dao.create(any(Object.class))).then(i -> i.getArgumentAt(0, Object.class));
    when(dao.create(anyList())).then(i -> i.getArgumentAt(0, ImmutableList.class));
    daoHelper = new DAOHelper(dao, clock, userProvider, changeTracker, searchIndexer);
  }

  @Test
  public void createPojo() {
    Pojo pojo = daoHelper.create(new Pojo());

    verify(dao).create(pojo);

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackCreated(any(ChangeTrackable.class));
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void updatePojo() {
    when(dao.update(eq(Pojo.class), eq(75L), any()))
        .then(i -> i.getArgumentAt(2, UnaryOperator.class).apply(new Pojo()));

    Pojo pojo = daoHelper.update(Pojo.class, 75L, identity());

    assertThat(pojo).isNotNull();
    verify(dao).update(eq(Pojo.class), eq(75L), any());

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackUpdated(any(), any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deletePojo() {
    daoHelper.delete(Pojo.class, 75L);

    verify(dao).delete(Pojo.class, 75L);

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackDeleted(any(ChangeTrackable.class));
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModTimes() {
    ModTimes modTimes = daoHelper.create(new ModTimes());

    verify(dao).create(modTimes);

    // ModTimes only uses clock.
    verify(clock).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackCreated(any(ChangeTrackable.class));
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().getMillis()).isEqualTo(1000L);
    assertThat(modTimes.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void updateModTimes() {
    when(dao.update(eq(ModTimes.class), eq(75L), any())).then(i ->
        i.getArgumentAt(2, UnaryOperator.class).apply(
            new ModTimes().setCreationTime(new DateTime(500L))));

    ModTimes modTimes = daoHelper.update(ModTimes.class, 75L, identity());

    verify(dao).update(eq(ModTimes.class), eq(75L), any());

    // Pojo does not implement any interfaces so should invoke no mocks.
    verify(clock).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackUpdated(any(), any());
    verify(searchIndexer, never()).index(any());

    assertThat(modTimes.getCreationTime().getMillis()).isEqualTo(500L);
    assertThat(modTimes.getLastModifiedTime().getMillis()).isEqualTo(1000L);
    assertThat(modTimes.getDeletionTime()).isNull();
  }

  @Test
  public void deleteModTimes() {
    daoHelper.delete(ModTimes.class, 75L);

    verify(dao).delete(ModTimes.class, 75L);

    // Modtimes actually does not support deletiontime
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackDeleted(any(ChangeTrackable.class));
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createModUser() {
    ModUser modUser = daoHelper.create(new ModUser());

    verify(dao).create(modUser);

    // Moduser uses clock and userProvider.
    verify(clock).millis();
    verify(userProvider).get();
    verify(changeTracker, never()).trackCreated(any(ChangeTrackable.class));
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
    when(dao.update(eq(ModUser.class), eq(75L), any())).then(i ->
        i.getArgumentAt(2, UnaryOperator.class).apply(
            new ModUser().setCreationUserId(501L).setCreationTime(new DateTime(500L))));

    ModUser modUser = daoHelper.update(ModUser.class, 75L, identity());

    verify(dao).update(eq(ModUser.class), eq(75L), any());

    // Moduser uses clock and userProvider.
    verify(clock).millis();
    verify(userProvider).get();
    verify(changeTracker, never()).trackUpdated(any(), any());
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
    daoHelper.delete(ModUser.class, 75L);

    verify(dao).delete(ModUser.class, 75L);

    // Moduser actually does not support deletion.
    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackDeleted(any(ChangeTrackable.class));
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createTrackable() {
    Trackable trackable = daoHelper.create(new Trackable());

    verify(dao).create(trackable);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker).trackCreated(trackable);
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void updateTrackable() {
    when(dao.update(eq(Trackable.class), eq(75L), any()))
        .then(i -> i.getArgumentAt(2, UnaryOperator.class).apply(new Trackable()));

    Trackable trackable = daoHelper.update(Trackable.class, 75L, identity());

    verify(dao).update(eq(Trackable.class), eq(75L), any());

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker).trackUpdated(eq(trackable), any());
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void deleteTrackable() {
    Trackable trackable = new Trackable();
    when(dao.get(Trackable.class, 75L)).thenReturn(trackable);

    daoHelper.delete(Trackable.class, 75L);

    verify(dao).delete(Trackable.class, 75L);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker).trackDeleted(trackable);
    verify(searchIndexer, never()).index(any());
  }

  @Test
  public void createSearchable() {
    SearchableObj searchable = daoHelper.create(new SearchableObj());

    verify(dao).create(searchable);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackCreated(any(ChangeTrackable.class));
    verify(searchIndexer).index(ImmutableList.of(searchable));
  }

  @Test
  public void updateSearchable() {
    when(dao.update(eq(SearchableObj.class), eq(75L), any()))
        .then(i -> i.getArgumentAt(2, UnaryOperator.class).apply(new SearchableObj()));

    SearchableObj searchable = daoHelper.update(SearchableObj.class, 75L, identity());

    verify(dao).update(eq(SearchableObj.class), eq(75L), any());

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackUpdated(any(), any());
    verify(searchIndexer).index(ImmutableList.of(searchable));
  }

  @Test
  public void deleteSearhable() {
    SearchableObj searchable = new SearchableObj();
    when(dao.get(SearchableObj.class, 75L)).thenReturn(searchable);

    daoHelper.delete(SearchableObj.class, 75L);

    verify(dao).delete(SearchableObj.class, 75L);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker, never()).trackDeleted(any(ChangeTrackable.class));
    verify(searchIndexer).removeIndex(SearchableObj.class, ImmutableList.of(75L));
  }

  @Test
  public void createSubClassAll() {
    SubAll subAll = daoHelper.create(new SubAll());

    verify(dao).create(subAll);

    verify(clock).millis();
    verify(userProvider).get();
    verify(changeTracker).trackCreated(subAll);
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
    when(dao.update(eq(SubAll.class), eq(75L), any())).then(i ->
        i.getArgumentAt(2, UnaryOperator.class).apply(
            new SubAll().setCreationUserId(501L).setCreationTime(new DateTime(500L))));

    SubAll subAll = daoHelper.update(SubAll.class, 75L, identity());

    verify(dao).update(eq(SubAll.class), eq(75L), any());

    verify(clock).millis();
    verify(userProvider).get();
    verify(changeTracker).trackUpdated(eq(subAll), any());
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
    SubAll subAll = new SubAll();
    when(dao.get(SubAll.class, 75L)).thenReturn(subAll);
    daoHelper.delete(SubAll.class, 75L);

    verify(dao).delete(SubAll.class, 75L);

    verify(clock, never()).millis();
    verify(userProvider, never()).get();
    verify(changeTracker).trackDeleted(subAll);
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
    @Override
    public Long getId() {
      return 75L;
    }
  }
  
  public static class SearchableObj extends ModelObject<Long> implements Searchable {}

  public static class ImplAll extends ModUser implements ChangeTrackable<Long>, Searchable {
    @Override
    public Long getId() {
      return 75L;
    }
  }

  public static class SubAll extends ImplAll {}
}
