package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.util.Calculate;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class IdTokenResolverDD4Impl implements IdTokenResolver {
	private static final long SESSION_TIME = 30 * Calculate.ONE_MINUTE;
	private final Clock clock;
	private final Map<String, User> activeusers = new HashMap<>();

	IdTokenResolverDD4Impl(Clock clock) {
		this.clock = clock;
	}

	@Override
	public User resolve(String idToken) {
		long now = clock.millis();
		User user = activeusers.get(idToken);
		if (user == null) {
			return null;
		} else if (user.getExpTime() < now) {
			activeusers.remove(idToken);
			return null;
		} else if (user.getExpTime() < SESSION_TIME / 2 + now) {
			activeusers.put(idToken, user = user.toBuilder().setExpTime(now + SESSION_TIME).build());
		}
		return user;
	}

	public User put(User user) {
		String tokenId = String.valueOf((int) (Math.random() * Integer.MAX_VALUE));
		activeusers.put(tokenId, user = user.toBuilder()
				.setIdToken(tokenId)
				.setExpTime(clock.millis() + SESSION_TIME)
				.build());
		return user;
	}

	void remove(String idToken) {
		activeusers.remove(idToken);
	}
}
