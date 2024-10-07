package com.digitald4.common.server;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.common.server.service.ChangeHistoryService;
import com.digitald4.common.storage.ChangeTracker.Change;
import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.junit.Test;

public class ChangeHistoryServiceTest {

  @Test
  public void getChanges_noArray_toArray() {
    JSONObject rev1 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");
    JSONObject rev2 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}\n");

    ImmutableList<Change> changes = ChangeHistoryService.getChanges(rev2, rev1);
    assertThat(changes).hasSize(1);
  }

  @Test
  public void getChanges_withArrays() {
    JSONObject rev2 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}\n");
    JSONObject rev3 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");

    ImmutableList<Change> changes = ChangeHistoryService.getChanges(rev3, rev2);
    assertThat(changes).hasSize(1);
  }

  @Test
  public void getChanges_fromArray_toNoArray() {
    JSONObject rev8 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":75,\"regDate\":1713510000000,\"payFlat\":200,\"payConfigs\":[{\"unitPrice\":75,\"unit\":\"Hour\",\"name\":\"Hourly\"},{\"unitPrice\":200,\"unit\":\"Visit\",\"name\":\"Fixed\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Roc2Hr\"},{\"unitPrice\":150,\"unit\":\"Hour\",\"name\":\"Hourly Soc2Hr\"},{\"unitPrice\":150,\"unit\":\"Visit\",\"name\":\"Soc2Hr\"}],\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");
    JSONObject rev9 = new JSONObject("{\"mileageRate\":0.55,\"lastName\":\"Ross\",\"address\":{\"address\":\"212 W Mission Ct, Corona, CA 92882, USA\",\"latitude\":33.8603344,\"longitude\":-117.570817},\"notes\":\"New nurse\",\"payRate\":60,\"regDate\":1713510000000,\"payFlat\":200,\"firstName\":\"Dr Greg\",\"phoneNumber\":\"123-456-7890\",\"payRate2HrRoc\":150,\"payRate2HrSoc\":150,\"payFlat2HrRoc\":150,\"id\":6217038301233152,\"payFlat2HrSoc\":150,\"email\":\"greg.ross@doctors.com\",\"status\":\"Active\"}");

    ImmutableList<Change> changes = ChangeHistoryService.getChanges(rev9, rev8);
    assertThat(changes).hasSize(2);
  }
}
