package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.LoginRequest;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Provider;
import com.google.protobuf.Empty;
import org.json.JSONObject;

public class UserService extends SingleProtoService<User> {

	private final UserStore userStore;
	private final Provider<User> userProvider;
	private final IdTokenResolver idTokenResolver;

	UserService(UserStore userStore, Provider<User> userProvider, IdTokenResolver idTokenResolver) {
		super(userStore);
		this.userStore = userStore;
		this.userProvider = userProvider;
		this.idTokenResolver = idTokenResolver;
	}

	private User getActive() throws DD4StorageException {
		return userProvider.get();
	}

	private User login(LoginRequest loginRequest) throws DD4StorageException {
		User user = userStore.getBy(loginRequest.getUsername(), loginRequest.getPassword());
		if (user == null) {
			throw new DD4StorageException("Wrong username or password");
		}
		return ((IdTokenResolverDD4Impl) idTokenResolver).put(userStore.updateLastLogin(user));
	}

	private Empty logout() throws DD4StorageException {
		User user = userProvider.get();
		if (user != null) {
			((IdTokenResolverDD4Impl) idTokenResolver).remove(user.getIdToken());
		}
		return Empty.getDefaultInstance();
	}

	@Override
	public JSONObject performAction(String action, JSONObject jsonRequest) {
		switch (action) {
			case "active": return convertToJSON(getActive());
			case "login": return convertToJSON(login(transformJSONRequest(LoginRequest.getDefaultInstance(), jsonRequest)));
			case "logout": return convertToJSON(logout());
			default: return super.performAction(action, jsonRequest);
		}
	}

	public boolean requiresLogin(String action) {
		return !action.equals("login") && !action.equals("logout") && !action.equals("create") && !action.equals("active");
	}
}
