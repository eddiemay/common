package com.digitald4.common.util;

import com.digitald4.common.model.BasicUser;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class JSONUtilTest {

  @Test
  public void toJSON() {
    BasicUser basicUser = new BasicUser().setId(123L).setEmail("user@email.com");

    assertThat(JSONUtil.toJSON(basicUser).toString())
        .isEqualTo("{\"typeId\":0,\"id\":123,\"email\":\"user@email.com\"}");
  }

  @Test
  public void toJSON_withDouble() {
    ObjectWithDouble testObj = new ObjectWithDouble().setId(123L).setTitle("Webwork");

    assertThat(JSONUtil.toJSON(testObj).toString()).isEqualTo("{\"billingRate\":0,\"id\":123,\"title\":\"Webwork\"}");

    testObj.setBillingRate(72.5);
    assertThat(JSONUtil.toJSON(testObj).toString())
        .isEqualTo("{\"billingRate\":72.5,\"id\":123,\"title\":\"Webwork\"}");
  }

  public static class ObjectWithDouble {
    private long id;
    private String title;
    private double billingRate;

    public long getId() {
      return id;
    }

    public ObjectWithDouble setId(long id) {
      this.id = id;
      return this;
    }

    public String getTitle() {
      return title;
    }

    public ObjectWithDouble setTitle(String title) {
      this.title = title;
      return this;
    }

    public double getBillingRate() {
      return billingRate;
    }

    public ObjectWithDouble setBillingRate(double billingRate) {
      this.billingRate = billingRate;
      return this;
    }
  }
}
