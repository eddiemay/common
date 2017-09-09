package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.util.Calculate;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class IdTokenResolverDD4Impl implements IdTokenResolver {
	private static final long TIMEOUT = 30 * Calculate.ONE_MINUTE;
	private final Clock clock;
	private final Map<String, User> activeusers = new HashMap<>();

	public IdTokenResolverDD4Impl(Clock clock) {
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
		}
		activeusers.put(idToken, user = user.toBuilder().setExpTime(now + TIMEOUT).build());
		return user;
	}

	public User put(User user) {
		String tokenId = String.valueOf((int) (Math.random() * Integer.MAX_VALUE));
		activeusers.put(tokenId, user = user.toBuilder()
				.setIdToken(tokenId)
				.setExpTime(clock.millis() + TIMEOUT)
				.build());
		return user;
	}

	public IdTokenResolverDD4Impl remove(String idToken) {
		activeusers.remove(idToken);
		return this;
	}
}
