package com.digitald4.common.util;

import com.digitald4.common.proto.DD4Protos.User;

public class UserProvider implements Provider<User> {
	public static final ThreadLocal<User> userThreadLocal = new ThreadLocal<User>();
	
	public UserProvider set(User user) {
		userThreadLocal.set(user);
		return this;
	}
	
	public User get() {
		return userThreadLocal.get();
	}
}
