package com.digitald4.common.server;

import com.digitald4.common.proto.DD4Protos.User;

public interface IdTokenResolver {
	User resolve(String idToken);
}
