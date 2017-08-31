package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.User;
import java.util.HashMap;
import java.util.Map;

public class IdTokenResolverDD4Impl implements IdTokenResolver{
	private final Map<String, User> activeusers = new HashMap<>();

	@Override
	public User resolve(String idToken) {
		return activeusers.get(idToken);
	}

	public User put(User user) {
		user = user.toBuilder().setIdToken(String.valueOf((int) (Math.random() * Integer.MAX_VALUE))).build();
		activeusers.put(user.getIdToken(), user);
		return user;
	}

	public IdTokenResolverDD4Impl remove(String idToken) {
		activeusers.remove(idToken);
		return this;
	}
}
