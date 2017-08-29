package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.LoginRequest;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Provider;
import com.google.protobuf.Empty;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class UserService extends SingleProtoService<User> {

	private final UserStore userStore;
	private final Provider<HttpServletRequest> requestProvider;

	UserService(UserStore userStore, Provider<HttpServletRequest> requestProvider) {
		super(userStore);
		this.userStore = userStore;
		this.requestProvider = requestProvider;
	}

	private User getActive() throws DD4StorageException {
		return getConverter().apply((User) requestProvider.get().getSession().getAttribute("user"));
	}

	private User login(LoginRequest loginRequest) throws DD4StorageException {
		User user = userStore.getBy(loginRequest.getUsername(), loginRequest.getPassword());
		if (user == null) {
			throw new DD4StorageException("Wrong username or password");
		}
		requestProvider.get().getSession().setAttribute("user", userStore.updateLastLogin(user));
		return user;
	}

	private Empty logout() throws DD4StorageException {
		requestProvider.get().getSession().setAttribute("user", null);
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
