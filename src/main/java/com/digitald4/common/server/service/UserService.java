package com.digitald4.common.server.service;

import static com.digitald4.common.util.ProtoUtil.toJSON;
import static com.digitald4.common.util.ProtoUtil.toProto;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.UpdateRequest;
import com.digitald4.common.model.User;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.LoginRequest;
import com.digitald4.common.server.IdTokenResolver;
import com.digitald4.common.server.IdTokenResolverDD4Impl;
import com.digitald4.common.storage.UserStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

@Api(
		name = "users",
		version = "v1",
		namespace = @ApiNamespace(
				ownerDomain = "common.digitald4.com",
				ownerName = "common.digitald4.com"
		)
)
public class UserService extends SingleProtoService<User> {

	private final UserStore userStore;
	private final Provider<User> userProvider;
	private final IdTokenResolver idTokenResolver;

	@Inject
	public UserService(UserStore userStore, Provider<User> userProvider, IdTokenResolver idTokenResolver) {
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
			((IdTokenResolverDD4Impl) idTokenResolver).remove(user.getActiveSession().getIdToken());
		}
		return Empty.getDefaultInstance();
	}

	public static class UserJSONService implements JSONService {
		private final DD4Protos.User type = DD4Protos.User.getDefaultInstance();
		private final UserService userService;

		public UserJSONService(UserService userService) {
			this.userService = userService;
		}
		public boolean requiresLogin(String action) {
			return !action.equals("login") && !action.equals("logout") && !action.equals("create") && !action.equals("active");
		}

		@Override
		public JSONObject performAction(String action, JSONObject jsonRequest) {
			switch (action) {
				case "create":
					return toJSON(userService.create(new BasicUser(toProto(type, jsonRequest))));
				case "get":
					return toJSON(userService.get(jsonRequest.getInt("id")));
				/*case "list":
					return toJSON(userService.list(toProto(ListRequest.getDefaultInstance(), jsonRequest)).getResults()
					.stream().map(user -> user.toProto()).collect(Collectors.toList()));*/
				case "update":
					return toJSON(userService.update(
							jsonRequest.getLong("id"),
							new UpdateRequest<>(
									new BasicUser(toProto(type, jsonRequest.getJSONObject("entity"))),
									FieldMask.newBuilder().addPaths(jsonRequest.getString("updateMask")).build())));
				case "delete":
					return toJSON(userService.delete(jsonRequest.getInt("id")));
				case "batchDelete":
					return toJSON(userService.batchDelete(toProto(BatchDeleteRequest.getDefaultInstance(), jsonRequest)));
				case "active": return toJSON(userService.getActive());
				case "login": return toJSON(userService.login(toProto(LoginRequest.getDefaultInstance(), jsonRequest)));
				case "logout": return toJSON(userService.logout());
				default: throw new DD4StorageException("Invalid action: " + action, HttpServletResponse.SC_BAD_REQUEST);
			}
		}
	}
}
