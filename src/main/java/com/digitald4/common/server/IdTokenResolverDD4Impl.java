package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;
import com.digitald4.common.storage.ListResponse;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Calculate;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdTokenResolverDD4Impl implements IdTokenResolver {
	private static final long SESSION_TIME = 30 * Calculate.ONE_MINUTE;
	private final UserStore userStore;
	private final Clock clock;
	private final Map<String, User> activeusers = new HashMap<>();

	IdTokenResolverDD4Impl(UserStore userStore, Clock clock) {
		this.userStore = userStore;
		this.clock = clock;
	}

	@Override
	public User resolve(String idToken) {
		long now = clock.millis();
		User user = activeusers.get(idToken);
		if (user == null) {
			List<User> list = userStore.list(ListRequest.newBuilder()
					.addFilter(Filter.newBuilder().setColumn("id_token").setValue(idToken))
					.build()).getResultList();
			if (list.isEmpty()) {
				return null;
			}
			user = list.get(0);
		}
		if (user.getExpTime() < now) {
			activeusers.remove(idToken);
			userStore.update(user.getId(), user_ -> user_.toBuilder()
					.clearIdToken()
					.clearExpTime()
					.build());
			return null;
		} else if (user.getExpTime() < SESSION_TIME / 2 + now) {
			activeusers.put(idToken, user = userStore.update(user.getId(), user_ -> user_.toBuilder()
					.setExpTime(now + SESSION_TIME)
					.build()));
		}
		return user;
	}

	public User put(User user) {
		String tokenId = String.valueOf((int) (Math.random() * Integer.MAX_VALUE));
		activeusers.put(tokenId, user = userStore.update(user.getId(), user_ -> user_.toBuilder()
				.setIdToken(tokenId)
				.setExpTime(clock.millis() + SESSION_TIME)
				.build()));
		return user;
	}

	void remove(String idToken) {
		activeusers.remove(idToken);
	}
}
