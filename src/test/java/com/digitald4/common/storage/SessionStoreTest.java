package com.digitald4.common.storage;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.Password;
import com.digitald4.common.model.Session;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.time.Duration;

public class SessionStoreTest {
  private final DAOTestingImpl dao = new DAOTestingImpl();
  @Mock private final UserStore mockUserStore = mock(UserStore.class);
  @Mock private final PasswordStore mockPasswordStore = mock(PasswordStore.class);
  @Mock private final ProviderThreadLocalImpl mockUserProvider = mock(ProviderThreadLocalImpl.class);
  @Mock private final Clock clock = mock(Clock.class);

  private SessionStore<BasicUser> sessionStore = new SessionStore<>(() -> dao, mockUserStore,
      mockPasswordStore, mockUserProvider, Duration.ofSeconds(10), true, clock);

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
  public void create_passwordRequired() {
    try {
      sessionStore.create("user", null);
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(400, e.getErrorCode());
      assertEquals("Username and password required", e.getMessage());
    }
  }

  @Test
  public void create_passwordMustBeEncoded() {
    try {
      sessionStore.create("user", "password");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(400, e.getErrorCode());
      assertEquals("None encrypted password detected. Passwords must be encrypted", e.getMessage());
    }
  }

  @Test
  public void create_usernameNotFound() {
    try {
      sessionStore.create("user", "0123456789ABCDEF");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(401, e.getErrorCode());
      assertEquals("Wrong username or password", e.getMessage());
    }
  }

  @Test
  public void create_wrongPassword() {
    try {
      when(mockUserStore.getBy("username")).thenReturn(new BasicUser().setId(1L).setUsername("username"));
      when(mockPasswordStore.verify(1L, "0123456789ABCDEF")).thenThrow(PasswordStore.BAD_LOGIN);

      sessionStore.create("username", "0123456789ABCDEF");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertEquals(401, e.getErrorCode());
      assertEquals("Wrong username or password", e.getMessage());
    }
  }

  @Test
  public void create_success() {
    when(mockUserStore.getBy("username")).thenReturn(new BasicUser().setId(1L).setUsername("username"));
    when(clock.millis()).thenReturn(10000L);

    Session session = sessionStore.create("username", "0123456789ABCDEF");

    assertEquals(10000L, session.getStartTime().getMillis());
    assertEquals(20000L, session.getExpTime().getMillis());
    assertNull(session.getEndTime());
    assertEquals(Session.State.ACTIVE, session.getState());
  }

  @Test
  public void get() {
    when(mockUserStore.getBy("username")).thenReturn(new BasicUser().setId(1L).setUsername("username"));
    when(mockPasswordStore.list(any(Query.List.class))).thenReturn(
        QueryResult.of(ImmutableList.of(new Password().setDigest("0123456789ABCDEF")), 1, Query.forList()));
    when(clock.millis()).thenReturn(10000L).thenReturn(15000L);
    Session session = sessionStore.create("username", "0123456789ABCDEF");

    session = sessionStore.get(session.getId());
    assertEquals(10000L, session.startTime());
    assertEquals(20000L, session.expTime());
    assertNull(session.getEndTime());
    assertEquals(Session.State.ACTIVE, session.getState());
  }

  @Test
  public void get_closesOutExpiredSession() {
    when(mockUserStore.getBy("username")).thenReturn(new BasicUser().setId(1L).setUsername("username"));
    when(mockPasswordStore.list(any(Query.List.class))).thenReturn(
        QueryResult.of(ImmutableList.of(new Password().setDigest("0123456789ABCDEF")), 1, null));
    when(clock.millis()).thenReturn(10000L).thenReturn(25000L);
    Session session = sessionStore.create("username", "0123456789ABCDEF");

    session = sessionStore.get(session.getId());
    assertEquals(10000L, session.getStartTime().getMillis());
    assertEquals(20000L, session.expTime());
    assertEquals(25000L, session.endTime().longValue());
    assertEquals(Session.State.CLOSED, session.getState());
    assertNull(session.user());
  }

  @Test
  public void resolve_works() {
    when(mockUserStore.getBy("username")).thenReturn(new BasicUser().setId(1L).setUsername("username"));
    when(mockPasswordStore.list(any(Query.List.class))).thenReturn(
        QueryResult.of(ImmutableList.of(new Password().setDigest("0123456789ABCDEF")), 1, null));
    when(clock.millis()).thenReturn(10000L).thenReturn(11000L);
    Session session = sessionStore.create("username", "0123456789ABCDEF");

    session = sessionStore.resolve(session.getId(), true);
    assertEquals(10000L, session.startTime());
    assertEquals(20000L, session.expTime());
    assertNull(session.endTime());
    assertEquals(Session.State.ACTIVE, session.getState());
  }

  @Test
  public void resolve_extendsHalfwayOverSession() {
    when(mockUserStore.getBy("username")).thenReturn(new BasicUser().setId(1L).setUsername("username"));
    when(mockPasswordStore.list(any(Query.List.class))).thenReturn(
        QueryResult.of(ImmutableList.of(new Password().setDigest("0123456789ABCDEF")), 1, null));
    when(clock.millis()).thenReturn(10000L).thenReturn(15001L);
    Session session = sessionStore.create("username", "0123456789ABCDEF");

    session = sessionStore.resolve(session.getId(), true);
    assertEquals(10000L, session.getStartTime().getMillis());
    assertEquals(25001L, session.expTime());
    assertNull(session.getEndTime());
    assertEquals(Session.State.ACTIVE, session.getState());

    session = sessionStore.get(session.getId());
    assertEquals(10000L, session.getStartTime().getMillis());
    assertEquals(25001L, session.expTime());
    assertNull(session.getEndTime());
    assertEquals(Session.State.ACTIVE, session.getState());
  }

  @Test
  public void testStoreToJSON() {
    Session session = new Session()
        .setId("4567")
        .setUserId(123)
        .setStartTime(new DateTime(1000))
        .setExpTime(new DateTime(10000))
        .setState(Session.State.ACTIVE);

    JSONObject json = new JSONObject(session);

    assertEquals("4567", json.getString("id"));
    assertEquals(session.getUserId(), json.getLong("userId"));
    assertEquals(session.getStartTime().getMillis(), json.get("startTime"));
    assertEquals(session.getExpTime().getMillis(), json.getLong("expTime"));
    assertEquals(session.getState(), json.get("state"));
  }
}
