package com.digitald4.common.server.service;

import static com.digitald4.common.util.JSONUtil.toObject;
import static com.digitald4.common.util.ProtoUtil.toJSON;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.User;
import com.digitald4.common.server.IdTokenResolver;
import com.digitald4.common.server.IdTokenResolverDD4Impl;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.protobuf.Empty;
import java.time.Clock;
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
public class UserService<U extends User> extends EntityServiceImpl<U> implements JSONService{

	private final UserStore<U> userStore;
	private final Provider<U> userProvider;
	private final IdTokenResolver idTokenResolver;
	private final Clock clock;

	@Inject
	public UserService(UserStore<U> userStore, Provider<U> userProvider, IdTokenResolver idTokenResolver, Clock clock) {
		super(userStore);
		this.userStore = userStore;
		this.userProvider = userProvider;
		this.idTokenResolver = idTokenResolver;
		this.clock = clock;
	}

	public U getActive() {
		return userProvider.get();
	}

	public U login(LoginRequest loginRequest) {
		U user = userStore.getBy(loginRequest.getUsername());
		if (user == null) {
			throw new DD4StorageException("Wrong username or password", 401);
		}
		user.verifyPassword(loginRequest.getPassword());
		return (U) ((IdTokenResolverDD4Impl) idTokenResolver)
				.put(userStore.update(user.getId(), user_ -> (U) user_.updateLastLogin(clock)));
	}

	public Empty logout() {
		User user = userProvider.get();
		if (user != null) {
			((IdTokenResolverDD4Impl) idTokenResolver).remove(user.activeSession().getIdToken());
		}

		return Empty.getDefaultInstance();
	}


	public boolean requiresLogin(String action) {
		return !action.equals("login") && !action.equals("logout") && !action.equals("create") && !action.equals("active");
	}

	@Override
	public JSONObject performAction(String action, JSONObject jsonRequest) {
		switch (action) {
			case "create":
				return toJSON(create(JSONUtil.toObject((Class<U>) userStore.getType().getClass(), jsonRequest)));
			case "get":
				return toJSON(get(jsonRequest.optInt("id")));
			case "list":
				return toJSON(
						list(
								jsonRequest.optString("filter"), jsonRequest.optString("orderBy"),
								jsonRequest.optInt("pageSize"), jsonRequest.optInt("pageToken")));
			case "update":
				return toJSON(
						update(
								jsonRequest.getLong("id"),
								JSONUtil.toObject((Class<U>) userStore.getClass(), jsonRequest.optJSONObject("entity")),
								jsonRequest.getString("updateMask")));
			case "delete": return toJSON(delete(jsonRequest.getInt("id")));
			case "active": return toJSON(getActive());
			case "login": return toJSON(login(JSONUtil.toObject(LoginRequest.class, jsonRequest)));
			case "logout": return toJSON(logout());
			default: throw new DD4StorageException("Invalid action: " + action, HttpServletResponse.SC_BAD_REQUEST);
		}
	}
}
