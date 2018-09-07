package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.ActiveSession;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.Calculate;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class IdTokenResolverDD4Impl implements IdTokenResolver {
	private static final long SESSION_TIME = 30 * Calculate.ONE_MINUTE;
	private final Store<ActiveSession> activeSessionStore;
	private final Clock clock;
	private final Map<String, ActiveSession> activeSessions = new HashMap<>();

	@Inject
	IdTokenResolverDD4Impl(Store<ActiveSession> activeSessionStore, Clock clock) {
		this.activeSessionStore = activeSessionStore;
		this.clock = clock;
	}

	@Override
	public User resolve(String idToken) {
		ActiveSession activeSession = getActiveSession(idToken);
		if (activeSession == null) {
			return null;
		}
		return activeSession.getUser();
	}

	private ActiveSession getActiveSession(String idToken) {
		if (idToken == null) {
			return null;
		}
		long now = clock.millis();
		ActiveSession activeSession = activeSessions.get(idToken);
		if (activeSession == null) {
			List<ActiveSession> list = activeSessionStore.list(Query.newBuilder()
					.addFilter(Filter.newBuilder().setColumn("id_token").setValue(idToken))
					.build()).getResults();
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
			activeSessions.put(idToken, activeSession = activeSessionStore.update(activeSession.getId(),
					activeSession_ -> activeSession_.toBuilder()
							.setExpTime(now + SESSION_TIME)
							.build()));
		}
		return activeSession;
	}

	public User put(User user) {
		ActiveSession activeSession;
		user = user.toBuilder()
				.setIdToken(String.valueOf((int) (Math.random() * Integer.MAX_VALUE)))
				.setExpTime(clock.millis() + SESSION_TIME)
				.build();
		activeSessions.put(user.getIdToken(), activeSession = activeSessionStore.create(ActiveSession.newBuilder()
				.setIdToken(user.getIdToken())
				.setExpTime(user.getExpTime())
				.setUser(user)
				.build()));
		return activeSession.getUser();
	}

	void remove(String idToken) {
		ActiveSession activeSession = activeSessions.get(idToken);
		if (activeSession == null) {
			List<ActiveSession> list = activeSessionStore.list(Query.newBuilder()
					.addFilter(Filter.newBuilder().setColumn("id_token").setValue(idToken))
					.build()).getResults();
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
