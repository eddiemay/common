package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.Session;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.*;

public class SessionStoreTest {

  private SessionStore<BasicUser> sessionStore =
      new SessionStore<>(null, null, null, null, Duration.ofMinutes(30), null);

  @Test
  public void create_usernameRequired() {
    try {
      sessionStore.create(null, "0123456789ABCDEF");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(400, e.getErrorCode());
      assertEquals("Username and password required", e.getMessage());
    }
  }

  @Test
  public void create_passworedRequired() {
    try {
      sessionStore.create("user", null);
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(400, e.getErrorCode());
      assertEquals("Username and password required", e.getMessage());
    }
  }

  @Test
  public void create_passworedMustBeEncoded() {
    try {
      sessionStore.create("user", "password");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(400, e.getErrorCode());
      assertEquals("None encrypted password detected. Passwords must be encrypted", e.getMessage());
    }
  }

  @Test
  public void testStoreToJSON() {
    Session session = new Session()
        .setUserId(123)
        .setStartTime(new DateTime(1000))
        .setExpTime(10000)
        .setIdToken("4567")
        .setState(Session.State.ACTIVE);

    JSONObject json = new JSONObject(session);

    assertEquals(session.getUserId(), json.getLong("userId"));
    assertEquals(session.getStartTime(), json.get("startTime"));
    assertEquals(session.getExpTime(), json.getLong("expTime"));
    assertEquals(session.getIdToken(), json.getString("idToken"));
    assertEquals(session.getState(), json.get("state"));
  }
}
