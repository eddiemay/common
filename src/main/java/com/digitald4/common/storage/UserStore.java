package com.digitald4.common.storage;

import com.digitald4.common.model.User;
import com.google.common.collect.ImmutableList;

public interface UserStore<U extends User> extends Store<U> {

	default U getBy(String username)  {
		return list(Query.forValues((username.contains("@") ? "email" : "username") + "=" + username, null, 0, 0))
				.getResults()
				.stream()
				.findFirst()
				.orElse(null);
	}
}
