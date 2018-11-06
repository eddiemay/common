package com.digitald4.common.storage;

import com.digitald4.common.model.User;

public interface UserStore<U extends User> extends Store<U> {

	U getBy(String username);
}
