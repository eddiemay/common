package com.digitald4.common.storage;

import com.digitald4.common.model.User;

public interface UserStore<U extends User> extends Store<U, Long> {

	default U getBy(String username)  {
		return list(Query.forList(null, (username.contains("@") ? "email" : "username") + "=" + username, null, 0, 0))
				.getItems()
				.stream()
				.findFirst()
				.orElse(null);
	}
}
