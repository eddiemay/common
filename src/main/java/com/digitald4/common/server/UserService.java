package com.digitald4.common.server;

import static com.digitald4.common.util.ProtoUtil.toJSON;
import static com.digitald4.common.util.ProtoUtil.toProto;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.LoginRequest;
import com.digitald4.common.storage.UserStore;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.protobuf.Empty;
import javax.inject.Inject;
import javax.inject.Provider;
import org.json.JSONObject;

@Api(
		name = "user",
		version = "v1",
		namespace = @ApiNamespace(
				ownerDomain = "nbastats.digitald4.com",
				ownerName = "nbastats.digitald4.com"
		),
		// [START_EXCLUDE]
		issuers = {
				@ApiIssuer(
						name = "firebase",
						issuer = "https://securetoken.google.com/fantasy-predictor",
						jwksUri =
								"https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system"
										+ ".gserviceaccount.com"
				)
		}
		// [END_EXCLUDE]
)
public class UserService extends SingleProtoService<User> {

	private final UserStore userStore;
	private final Provider<User> userProvider;
	private final IdTokenResolver idTokenResolver;

	@Inject
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
