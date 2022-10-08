package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Session;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Annotations.SessionCacheEnabled;
import com.digitald4.common.storage.Annotations.SessionDuration;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import org.joda.time.DateTime;

public class SessionStore<U extends User> extends GenericStore<Session, String> implements LoginResolver {
  private static final DD4StorageException NOT_AUTHENICATED =
      new DD4StorageException("Not authenicated", ErrorCode.NOT_AUTHENTICATED);

  private final UserStore<U> userStore;
  private final PasswordStore passwordStore;
  private final ProviderThreadLocalImpl<U> userProvider;
  private final Duration sessionDuration;
  private final boolean sessionCacheEnabled;
  private final Clock clock;
  private final Map<String, Session> activeSessions;

  @Inject
  public SessionStore(
      Provider<DAO> daoProvider, UserStore<U> userStore, PasswordStore passwordStore,
      ProviderThreadLocalImpl<U> userProvider, @SessionDuration Duration sessionDuration,
      @SessionCacheEnabled boolean sessionCacheEnabled, Clock clock) {
    super(Session.class, daoProvider);
    this.userStore = userStore;
    this.passwordStore = passwordStore;
    this.userProvider = userProvider;
    this.sessionDuration = sessionDuration;
    this.sessionCacheEnabled = sessionCacheEnabled;
    this.activeSessions = sessionCacheEnabled ? new HashMap<>() : null;
    this.clock = clock;
  }

  public Session create(String username, String password) {
    if (username == null || password == null) {
      throw new DD4StorageException("Username and password required", ErrorCode.BAD_REQUEST);
    }

    PasswordStore.validateEncoding(password);

    User user = userStore.getBy(username);
    if (user == null) {
      throw PasswordStore.BAD_LOGIN;
    }

    passwordStore.verify(user.getId(), password);

    DateTime now = new DateTime(clock.millis());

    return cachePut(
        create(
            new Session()
                .setId(String.valueOf((int) (Math.random() * Integer.MAX_VALUE)))
                .setUserId(user.getId())
                .setStartTime(now)
                .setExpTime(now.plus(sessionDuration.toMillis()))
                .setState(Session.State.ACTIVE))
        .user(user));
  }

  public Session get(String token) {
    if (token == null) {
      return null;
    }

    DateTime now = new DateTime(clock.millis());
    Session activeSession = cacheGet(token);
    if (activeSession == null) {
      activeSession = super.get(token);

      if (activeSession == null || activeSession.getState() != Session.State.ACTIVE) {
        return null;
      }

      // Only set the user on active sessions.
      if (activeSession.getExpTime().isAfter(now)) {
        activeSession = cachePut(activeSession.user(userStore.get(activeSession.getUserId())));
      }
    }

    // If the session should be expired, expire it.
    if (activeSession.getState() != Session.State.CLOSED && activeSession.getExpTime().isBefore(now)) {
      activeSession = close(activeSession);
    }

    return activeSession;
  }

  public Session resolve(String token, boolean loginRequired) {
    Session session = get(token);
    if (loginRequired && (session == null || session.getState() != Session.State.ACTIVE)) {
      throw NOT_AUTHENICATED;
    }

    if (session == null) {
      return null;
    }

    DateTime now = new DateTime(clock.millis());
    long duration = sessionDuration.toMillis();
    // Extend the session if it is more than halfway over.
    if (session.getState() == Session.State.ACTIVE && session.getExpTime().isBefore(now.plus(duration / 2))) {
      session = cachePut(update(session.getId(), as -> as.setExpTime(now.plus(duration))).user(session.user()));
    }

    userProvider.set(session.user());
    return session;
  }

  public Session close(String tokenId) {
    return close(get(tokenId));
  }

  private Session close(Session session) {
    if (session == null) {
      return null;
    }

    return update(
        cacheRemove(session).getId(), s -> s.setEndTime(new DateTime(clock.millis())).setState(Session.State.CLOSED));
  }


  private Session cachePut(Session session) {
    if (sessionCacheEnabled) {
      activeSessions.put(session.getId(), session);
    }
    return session;
  }

  private Session cacheGet(String token) {
    return sessionCacheEnabled ? activeSessions.get(token) : null;
  }

  private Session cacheRemove(Session session) {
    if (sessionCacheEnabled) {
      activeSessions.remove(session.getId());
    }
    return session;
  }
}
