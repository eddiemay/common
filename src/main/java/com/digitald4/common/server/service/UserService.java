package com.digitald4.common.server.service;

import static com.digitald4.common.util.ProtoUtil.toJSON;

import com.digitald4.common.model.Session;
import com.digitald4.common.model.PasswordInfo;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.Store;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;

import java.time.Clock;
import javax.inject.Inject;
import org.json.JSONObject;

@Api(
		name = "users",
		version = "v1",
		namespace = @ApiNamespace(
				ownerDomain = "common.digitald4.com",
				ownerName = "common.digitald4.com"
		)
)
public class UserService<U extends User> extends EntityServiceImpl<U, U> {

	private final UserStore<U> userStore;
	private final SessionStore<U> sessionStore;
	private final Store<PasswordInfo> passwordStore;
	private final Clock clock;

	@Inject
	public UserService(
			UserStore<U> userStore, SessionStore<U> sessionStore, Store<PasswordInfo> passwordStore, Clock clock) {
		super(userStore, sessionStore, true);
		this.userStore = userStore;
		this.sessionStore = sessionStore;
		this.passwordStore = passwordStore;
		this.clock = clock;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "activeSession")
	public Session getActiveSession(@Named("idToken") String idToken) throws ServiceException {
		return sessionStore.get(idToken);
	}

	public Session login(LoginRequest loginRequest) throws ServiceException {
		return sessionStore.create(loginRequest.getUsername(), loginRequest.getPassword().toUpperCase());
	}

	public Session logout(@Named("idToken") String idToken) {
		return sessionStore.close(idToken);
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "updatePassword")
	public Empty updatePassword(
			PasswordUpdateRequest updatePaswordRequest, @Named("idToken") String idToken) throws ServiceException {
		sessionStore.resolve(idToken, true);
		long userId = updatePaswordRequest.getUserId();
		String password = updatePaswordRequest.getPassword().toUpperCase();

		PasswordInfo passwordInfo = passwordStore
				.list(new Query().setFilters(new Query.Filter().setColumn("userId").setOperator("=").setValue(userId)))
				.getResults()
				.stream()
				.findFirst().orElse(null);
		if (passwordInfo != null) {
			passwordStore.update(passwordInfo.getId(), pi -> pi.setDigest(password).setLastUpdated(clock.millis()));
		} else {
			U user = userStore.get(userId);
			if (user == null) {
				throw new NotFoundException("User not found");
			}

			passwordStore.create(new PasswordInfo().setUserId(userId).setDigest(password).setLastUpdated(clock.millis()));
		}

		return Empty.getInstance();
	}

	public static class PasswordUpdateRequest {
		private long userId;
		private String password;

		public long getUserId() {
			return userId;
		}

		public PasswordUpdateRequest setUserId(long userId) {
			this.userId = userId;
			return this;
		}

		public String getPassword() {
			return password;
		}

		public PasswordUpdateRequest setPassword(String password) {
			this.password = password;
			return this;
		}
	}

	public static class UserJSONService<U extends User> extends JSONServiceHelper<U> {

		private UserService<U> userService;
		public UserJSONService(UserService<U> userService) {
			super(userService);
			this.userService = userService;
		}

		@Override
		public JSONObject performAction(String action, JSONObject jsonRequest) throws ServiceException {
			switch (action) {
				case "activeSession":
					return toJSON(userService.getActiveSession(jsonRequest.getString("idToken")));
				case "login":
					return toJSON(userService.login(JSONUtil.toObject(LoginRequest.class, jsonRequest)));
				case "logout":
					return toJSON(userService.logout(jsonRequest.getString("idToken")));
				default:
					return super.performAction(action, jsonRequest);
			}
		}
	}
}
