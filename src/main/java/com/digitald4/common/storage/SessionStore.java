package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.PasswordInfo;
import com.digitald4.common.model.Session;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Annotations.SessionDuration;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SessionStore<U extends User> extends GenericStore<Session> {
  private static final DD4StorageException BAD_LOGIN =
      new DD4StorageException("Wrong username or password", ErrorCode.NOT_AUTHENTICATED);
  private static final DD4StorageException NOT_AUTHENICATED =
      new DD4StorageException("Not authenicated", ErrorCode.NOT_AUTHENTICATED);

  private final UserStore<U> userStore;
  private final PasswordStore passwordStore;
  private final ProviderThreadLocalImpl<U> userProvider;
  private final Duration sessionDuration;
  private final Clock clock;
  private final Map<String, Session> activeSessions = new HashMap<>();

  @Inject
  public SessionStore(Provider<DAO> daoProvider, UserStore<U> userStore, PasswordStore passwordStore,
                      ProviderThreadLocalImpl<U> userProvider, @SessionDuration Duration sessionDuration, Clock clock) {
    super(Session.class, daoProvider);
    this.userStore = userStore;
    this.passwordStore = passwordStore;
    this.userProvider = userProvider;
    this.sessionDuration = sessionDuration;
    this.clock = clock;
  }

  public Session create(String username, String password) {
    if (username == null || password == null) {
      throw new DD4StorageException("Username and password required", ErrorCode.BAD_REQUEST);
    }

    PasswordStore.validate(password);

    User user = userStore.getBy(username);
    if (user == null) {
      throw BAD_LOGIN;
    }

    PasswordInfo passwordInfo = passwordStore
        .list(new Query().setFilters(new Query.Filter().setColumn("userId").setOperator("=").setValue(user.getId())))
        .getResults().stream().findFirst().orElse(null);
    if (passwordInfo == null || !passwordInfo.getDigest().equals(password)) {
      throw BAD_LOGIN;
    }

    DateTime now = new DateTime(clock.millis());
    Session session = create(
        new Session()
            .setIdToken(String.valueOf((int) (Math.random() * Integer.MAX_VALUE)))
            .setUserId(user.getId())
            .setStartTime(now)
            .setExpTime(now.plus(sessionDuration.toMillis()).getMillis())
            .setState(Session.State.ACTIVE));
    activeSessions.put(session.getIdToken(), session.user(user));

    return session;
  }

  public Session get(String token) {
    if (token == null) {
      return null;
    }

    long now = clock.millis();
    Session activeSession = activeSessions.computeIfAbsent(
        token,
        t -> list(new Query().setFilters(new Query.Filter().setColumn("idToken").setOperator("=").setValue(token)))
            .getResults()
            .stream()
            .peek(session -> {
              // Only set the user on active sessions.
              if (session.getState() == Session.State.ACTIVE && session.getExpTime() > now) {
                session.user(userStore.get(session.getUserId()));
              }
            })
            .findFirst()
            .orElseThrow(() -> NOT_AUTHENICATED));

    // If the session should be expired, expire it.
    if (activeSession.getState() != Session.State.CLOSED && activeSession.getExpTime() < now) {
      activeSession = close(activeSession);
    }

    return activeSession;
  }

  public Session resolve(String token, boolean loginRequired) {
    Session session = get(token);
    if (session == null) {
      if (loginRequired) {
        throw NOT_AUTHENICATED;
      }
      return null;
    }

    long now = clock.millis();
    long duration = sessionDuration.toMillis();
    // Extend the session if it is more than halfway over.
    if (session.getState() == Session.State.ACTIVE && session.getExpTime() < duration / 2 + now) {
      activeSessions.put(
          token, session = update(session.getId(), as -> as.setExpTime(now + duration)).user(session.user()));
    }

    if (loginRequired && session.getState() != Session.State.ACTIVE) {
      throw NOT_AUTHENICATED;
    }

    userProvider.set(session.user());
    return session;
  }

  public Session close(String tokenId) {
    return close(get(tokenId));
  }

  private Session close(Session session) {
    activeSessions.remove(session.getIdToken());
    return update(session.getId(), s -> s.setEndTime(clock.millis()).setState(Session.State.CLOSED));
  }
}
