package com.digitald4.common.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.util.function.UnaryOperator.identity;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.ChangeTrackable;
import com.digitald4.common.model.User;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.ChangeTracker.Change;
import com.digitald4.common.storage.ChangeTracker.ChangeHistory;
import com.digitald4.common.storage.Transaction.Op;
import com.digitald4.common.storage.Transaction.Op.Action;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ChangeTrackerTest {
  private final DAO dao = mock(DAO.class);
  private final User user = new BasicUser().setId(1001L).setUsername("user1");
  private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  private final Clock clock = mock(Clock.class);
  private ChangeTracker changeTracker;

  @Before
  public void setup() {
    when(clock.millis()).thenReturn(1000L);
    when(dao.persist(any(Transaction.class))).then(i -> i.getArgument(0));
    changeTracker = new ChangeTracker(() -> user, null, searchIndexer, clock);
  }

  @Test
  public void trackCreated() {
    ChangeTrackableUser trackable = new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");

    ChangeHistory changeHistory = changeTracker.createChangeHistory(Op.create(trackable)).getEntity();

    assertThat(changeHistory.getAction()).isEqualTo(Action.CREATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity().toString()).isEqualTo(new JSONObject(trackable).toString());
  }

  @Test
  public void trackUpdated() {
    ChangeTrackableUser updated = new ChangeTrackableUser().setId(1002L)
        .setUsername(null).setFirstName("FirstName").setLastName("LastName");

    ChangeHistory changeHistory = changeTracker
        .createChangeHistory(Op.update(ChangeTrackableUser.class,1002L, identity()).setEntity(updated))
        .getEntity();

    assertThat(changeHistory.getAction()).isEqualTo(Action.UPDATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity().toString()).isEqualTo(new JSONObject(updated).toString());
  }

  @Test
  public void trackDeleted() {
    var trackable = new ChangeTrackableUser().setUsername("user2").setId(1002L).setFirstName("First");
    when(dao.get(ChangeTrackableUser.class, ImmutableList.of(1002L))).thenReturn(
        BulkGetable.MultiListResult.of(ImmutableList.of(trackable), ImmutableList.of(1002L)));

    ChangeHistory changeHistory =
        changeTracker.trackDeleted(dao, ChangeTrackableUser.class, ImmutableList.of(1002L)).get(0);

    assertThat(changeHistory.getAction()).isEqualTo(Action.DELETED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getEntity().toString()).isEqualTo(new JSONObject(trackable).toString());
  }

  @Test
  public void withTestDao() {
    changeTracker = new ChangeTracker(() -> user, null, searchIndexer, clock);
    DAOTestingImpl testingDao = new DAOTestingImpl(changeTracker);

    ChangeTrackableUser trackable =
        new ChangeTrackableUser().setId(1002L).setUsername("user2").setFirstName("First");

    changeTracker.postPersist(testingDao, Transaction.of(Op.create(trackable)));

    ChangeHistory changeHistory = testingDao.get(ChangeHistory.class, 5001L);

    assertThat(changeHistory.getAction()).isEqualTo(Action.CREATED);
    assertThat(changeHistory.getEntityType()).isEqualTo("ChangeTrackableUser");
    assertThat(changeHistory.getEntityId()).isEqualTo("1002");
    assertThat(changeHistory.getUserId()).isEqualTo(1001L);
    assertThat(changeHistory.getUsername()).isEqualTo("user1");
    assertThat(changeHistory.getChanges()).isNull();
  }

  @Test
  public void getChanges_noArray_toArray() {
    JSONObject rev1 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");
    JSONObject rev2 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}\n");

    ImmutableList<Change> changes = ChangeTracker.commuteChanges(rev2, rev1);
    assertThat(changes).hasSize(1);
  }

  @Test
  public void getChanges_withArrays() {
    JSONObject rev2 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}\n");
    JSONObject rev3 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");

    ImmutableList<Change> changes = ChangeTracker.commuteChanges(rev3, rev2);
    assertThat(changes).hasSize(1);
  }

  @Test
  public void getChanges_fromArray_toNoArray() {
    JSONObject rev8 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");
    JSONObject rev9 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":60,\"regDate\":1713510000000,\"payFlat\":200,\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");

    ImmutableList<Change> changes = ChangeTracker.commuteChanges(rev9, rev8);
    assertThat(changes).hasSize(2);
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
