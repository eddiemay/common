package com.digitald4.common.server;

import static com.digitald4.common.util.ProtoUtil.toJSON;
import static com.digitald4.common.util.ProtoUtil.toProto;

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

	public User getActive() {
		return userProvider.get();
	}

	public User login(LoginRequest loginRequest) {
		User user = userStore.getBy(loginRequest.getUsername(), loginRequest.getPassword());
		if (user == null) {
			throw new DD4StorageException("Wrong username or password", 401);
		}
		return ((IdTokenResolverDD4Impl) idTokenResolver).put(userStore.updateLastLogin(user));
	}

	public Empty logout() {
		User user = userProvider.get();
		if (user != null) {
			((IdTokenResolverDD4Impl) idTokenResolver).remove(user.getIdToken());
		}
		return Empty.getDefaultInstance();
	}

	static class UserJSONService extends JSONServiceImpl<User> {
		private final UserService userService;

		public UserJSONService(UserService userService) {
			super(User.class, userService, false);
			this.userService = userService;
		}
		public boolean requiresLogin(String action) {
			return !action.equals("login") && !action.equals("logout") && !action.equals("create") && !action.equals("active");
		}

		@Override
		public JSONObject performAction(String action, JSONObject jsonRequest) {
			switch (action) {
				case "active": return toJSON(userService.getActive());
				case "login": return toJSON(userService.login(toProto(LoginRequest.getDefaultInstance(), jsonRequest)));
				case "logout": return toJSON(userService.logout());
				default: return super.performAction(action, jsonRequest);
			}
		}
	}
}
