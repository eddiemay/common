package com.digitald4.common.server;

import com.digitald4.common.model.ActiveSession;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Store;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Calculate;
import com.google.common.collect.ImmutableList;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

public class IdTokenResolverDD4Impl<U extends User> implements IdTokenResolver<U> {
	private static final long SESSION_TIME = 30 * Calculate.ONE_MINUTE;
	private final Store<ActiveSession> activeSessionStore;
	private final UserStore<U> userStore;
	private final Clock clock;
	private final Map<String, ActiveSession> activeSessions = new HashMap<>();

	@Inject
	IdTokenResolverDD4Impl(Store<ActiveSession> activeSessionStore, UserStore<U> userStore, Clock clock) {
		this.activeSessionStore = activeSessionStore;
		this.userStore = userStore;
		this.clock = clock;
	}

	@Override
	public U resolve(String idToken) {
		ActiveSession activeSession = getActiveSession(idToken);
		if (activeSession == null) {
			return null;
		}

		return (U) userStore.get(activeSession.getUserId()).activeSession(activeSession);
	}

	private ActiveSession getActiveSession(String idToken) {
		if (idToken == null) {
			return null;
		}
		long now = clock.millis();
		ActiveSession activeSession = activeSessions.get(idToken);
		if (activeSession == null) {
			ImmutableList<ActiveSession> list = activeSessionStore
					.list(new Query().setFilters(new Filter().setColumn("id_token").setOperator("=").setValue(idToken)))
					.getResults();
			if (list.isEmpty()) {
				return null;
			}
			activeSession = list.get(0);
			activeSessions.put(activeSession.getIdToken(), activeSession);
		}
		if (activeSession.getExpTime() < now) {
			remove(idToken);
			return null;
		} else if (activeSession.getExpTime() < SESSION_TIME / 2 + now) {
			activeSessions.put(idToken,
					activeSession = activeSessionStore.update(activeSession.getId(),
							activeSession_ -> activeSession_.setExpTime(now + SESSION_TIME)));
		}
		return activeSession;
	}

	public User put(User user) {
		String idToken = String.valueOf((int) (Math.random() * Integer.MAX_VALUE));
		return user.activeSession(
				activeSessions.put(idToken,
						activeSessionStore.create(new ActiveSession()
								.setIdToken(idToken)
								.setExpTime(clock.millis() + SESSION_TIME)
								.setUserId(user.getId()))));
	}

	public void remove(String idToken) {
		ActiveSession activeSession = activeSessions.get(idToken);
		if (activeSession == null) {
			ImmutableList<ActiveSession> list = activeSessionStore
					.list(new Query().setFilters(new Filter().setColumn("id_token").setValue(idToken)))
					.getResults();
			if (!list.isEmpty()) {
				activeSession = list.get(0);
			}
		}
		if (activeSession != null) {
			activeSessions.remove(idToken);
			activeSessionStore.delete(activeSession.getId());
		}
	}
}
