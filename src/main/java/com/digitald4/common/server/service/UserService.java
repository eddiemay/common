package com.digitald4.common.server.service;

import static com.digitald4.common.util.JSONUtil.toJSON;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.Session;
import com.digitald4.common.model.User;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.JSONUtil;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;
import javax.inject.Inject;
import org.json.JSONObject;

@Api(
		name = "users",
		version = "v1",
		namespace = @ApiNamespace(ownerDomain = "common.digitald4.com", ownerName = "common.digitald4.com")
)
public class UserService<U extends User> extends EntityServiceImpl<U, Long> {
	private final UserStore<U> userStore;
	private final SessionStore<U> sessionStore;
	private final PasswordStore passwordStore;

	@Inject
	public UserService(UserStore<U> userStore, SessionStore<U> sessionStore, PasswordStore passwordStore) {
		super(userStore, sessionStore);
		this.userStore = userStore;
		this.sessionStore = sessionStore;
		this.passwordStore = passwordStore;
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "activeSession")
	public Session getActiveSession(@Named("idToken") String idToken) throws ServiceException {
		try {
			return sessionStore.get(idToken);
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST)
	public Session login(LoginRequest loginRequest) throws ServiceException {
		try {
			return sessionStore.create(loginRequest.getUsername(), loginRequest.getPassword());
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "logout")
	public Session logout(@Named("idToken") String idToken) throws ServiceException {
		try {
			return sessionStore.close(idToken);
		} catch (DD4StorageException e) {
			throw new ServiceException(e.getErrorCode(), e);
		}
	}

	@ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "updatePassword")
	public Empty updatePassword(PasswordUpdateRequest updatePasswordRequest,
			@Named("idToken") String idToken) throws ServiceException {
		sessionStore.resolve(idToken, true);
		String username = updatePasswordRequest.getUsername();
		String password = updatePasswordRequest.getPassword();

		PasswordStore.validateEncoding(password);

		U user = userStore.getBy(username);
		if (user == null) {
			throw new NotFoundException("User not found");
		}

		passwordStore.updatePassword(user.getId(), password);

		return Empty.getInstance();
	}

	public static class PasswordUpdateRequest {
		private String username;
		private String password;

		public String getUsername() {
			return username;
		}

		public PasswordUpdateRequest setUsername(String username) {
			this.username = username;
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
		private final UserService<U> userService;
		public UserJSONService(UserService<U> userService) {
			super(userService);
			this.userService = userService;
		}

		@Override
		public JSONObject performAction(String action, JSONObject jsonRequest) throws ServiceException {
			return switch (action) {
				case "activeSession" -> toJSON(userService.getActiveSession(jsonRequest.getString("idToken")));
				case "login" -> toJSON(userService.login(JSONUtil.toObject(LoginRequest.class, jsonRequest)));
				case "logout" -> toJSON(userService.logout(jsonRequest.getString("idToken")));
				default -> super.performAction(action, jsonRequest);
			};
		}
	}
}
