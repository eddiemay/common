package com.digitald4.common.server;

import com.digitald4.common.model.User;

public interface IdTokenResolver {
	User resolve(String idToken);
}
