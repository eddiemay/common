package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.ChangeTracker.Change;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory.Action;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.LinkedHashMap;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;

public class ChangeTrackerTest {

  private final DAO dao = mock(DAO.class);
  private final User user = new BasicUser().setId(1001L).setUsername("user1");
  private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  private final Clock clock = mock(Clock.class);
  private ChangeTracker changeTracker;

  private DAOTestingImpl testingDao;
  private Provider<DAO> testDaoProvider = () -> testingDao;

  @Before
  public void setup() {
    when(clock.millis()).thenReturn(1000L);
    when(dao.create(any(Object.class))).then(i -> i.getArgument(0));
    when(dao.create(anyList())).then(i -> i.getArgument(0));
    changeTracker = new ChangeTracker(() -> dao, () -> user, searchIndexer, clock);
  }

  @Test
  public void trackCreated() {
    when(dao.get(ChangeTrackableUser.class, ImmutableList.of(1002L)))
        .thenReturn(ImmutableList.of());

    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");

    ChangeHistory changeHistory = changeTracker.trackRevised(ImmutableList.of(trackable), true).get(0);

    assertThat(changeHistory.getAction()).isEqualTo(Action.CREATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity()).isEqualTo(trackable);
    assertThat(changeHistory.getChanges()).isNull();
  }

  @Test
  public void trackUpdated() {
    when(dao.get(ChangeTrackableUser.class, ImmutableList.of(1002L))).thenReturn(
        ImmutableList.of(
            new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First")));

    ChangeTrackableUser updated = new ChangeTrackableUser().setId(1002L)
        .setUsername(null).setFirstName("FirstName").setLastName("LastName");

    ChangeHistory changeHistory = changeTracker.trackRevised(ImmutableList.of(updated), false).get(0);

    assertThat(changeHistory.getAction()).isEqualTo(Action.UPDATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity()).isEqualTo(updated);
  }

  @Test
  public void trackDeleted() {
    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setUsername("user2").setId(1002L).setFirstName("First");
    when(dao.get(ChangeTrackableUser.class, ImmutableList.of(1002L)))
        .thenReturn(ImmutableList.of(trackable));

    ChangeHistory changeHistory =
        changeTracker.trackDeleted(ChangeTrackableUser.class, ImmutableList.of(1002L)).get(0);

    assertThat(changeHistory.getAction()).isEqualTo(Action.DELETED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity()).isEqualTo(trackable);
    assertThat(changeHistory.getChanges()).isNull();
  }

  @Test
  public void withTestDao() {
    changeTracker = new ChangeTracker(testDaoProvider, () -> user, searchIndexer, clock);
    testingDao = new DAOTestingImpl(changeTracker);

    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");

    ChangeHistory changeHistory = changeTracker.trackRevised(ImmutableList.of(trackable), true).get(0);

    changeHistory = testingDao.get(ChangeHistory.class, changeHistory.getId());

    assertThat(changeHistory.getAction()).isEqualTo(Action.CREATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity().getClass()).isEqualTo(LinkedHashMap.class);
    assertThat(changeHistory.getChanges()).isNull();
  }

  public static class ChangeTrackableUser extends BasicUser implements ChangeTrackable<Long> {
    @Override
    public ChangeTrackableUser setId(Long id) {
      super.setId(id);
      return this;
    }

    @Override
    public ChangeTrackableUser setUsername(String username) {
      super.setUsername(username);
      return this;
    }

    @Override
    public ChangeTrackableUser setFirstName(String firstName) {
      super.setFirstName(firstName);
      return this;
    }

    @Override
    public ChangeTrackableUser setLastName(String lastName) {
      super.setLastName(lastName);
      return this;
    }
  }
}
