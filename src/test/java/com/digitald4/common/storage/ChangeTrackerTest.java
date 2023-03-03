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
import com.digitald4.common.storage.ChangeTracker.ChangeHistory.ChangeType;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.LinkedHashMap;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;

public class ChangeTrackerTest {

  private final DAO dao = mock(DAO.class);
  private final User user = new BasicUser().setId(1001L).setUsername("user1");
  private final Provider<User> userProvider = () -> user;
  private final Clock clock = mock(Clock.class);
  private ChangeTracker changeTracker;

  @Before
  public void setup() {
    when(clock.millis()).thenReturn(1000L);
    when(dao.create(any(Object.class))).then(i -> i.getArgumentAt(0, ChangeTrackable.class));
    when(dao.create(anyList())).then(i -> i.getArgumentAt(0, ImmutableList.class));
    changeTracker = new ChangeTracker(dao, userProvider, clock);
  }

  @Test
  public void trackCreated() {
    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");

    ChangeHistory changeHistory = changeTracker.trackCreated(trackable);

    assertThat(changeHistory.getChangeType()).isEqualTo(ChangeType.CREATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.<Long>getEntityId()).isEqualTo(1002L);
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity()).isEqualTo(trackable);
    assertThat(changeHistory.getChanges()).isNull();
  }

  @Test
  public void trackUpdated() {
    ChangeTrackableUser original =
        new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");
    ChangeTrackableUser updated = new ChangeTrackableUser().setId(1002L)
        .setUsername(null).setFirstName("FirstName").setLastName("LastName");

    ChangeHistory changeHistory = changeTracker.trackUpdated(updated, JSONUtil.toJSON(original));

    assertThat(changeHistory.getChangeType()).isEqualTo(ChangeType.UPDATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.<Long>getEntityId()).isEqualTo(1002L);
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity()).isEqualTo(updated);
    assertThat(changeHistory.getChanges()).containsExactly(
        Change.create("username", "user2"),
        Change.create("firstName", "First"),
        Change.create("lastName", null));

  }

  @Test
  public void trackDeleted() {
    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setUsername("user2").setId(1002L).setFirstName("First");

    ChangeHistory changeHistory = changeTracker.trackDeleted(trackable);

    assertThat(changeHistory.getChangeType()).isEqualTo(ChangeType.DELETED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.<Long>getEntityId()).isEqualTo(1002L);
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity()).isEqualTo(trackable);
    assertThat(changeHistory.getChanges()).isNull();

  }

  @Test
  public void withTestDao() throws Exception {
    DAOTestingImpl dao = new DAOTestingImpl();
    changeTracker = new ChangeTracker(dao, userProvider, clock);

    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");

    ChangeHistory changeHistory = changeTracker.trackCreated(trackable);

    changeHistory = dao.get(ChangeHistory.class, changeHistory.getId());

    assertThat(changeHistory.getChangeType()).isEqualTo(ChangeType.CREATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.<Integer>getEntityId()).isEqualTo(1002L);
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
