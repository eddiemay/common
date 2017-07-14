package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4UIProtos;
import com.digitald4.common.proto.DD4UIProtos.LoginRequest;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Provider;
import com.google.protobuf.Empty;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

public class UserService extends DualProtoService<DD4UIProtos.User, DD4Protos.User> {

	private final UserStore userStore;
	private final Provider<HttpServletRequest> requestProvider;

	UserService(UserStore userStore, Provider<HttpServletRequest> requestProvider) {
		super(DD4UIProtos.User.class, userStore);
		this.userStore = userStore;
		this.requestProvider = requestProvider;
	}

	public DD4UIProtos.User getActive() throws DD4StorageException {
		return getConverter().apply((DD4Protos.User) requestProvider.get().getSession(true).getAttribute("puser"));
	}

	public boolean login(LoginRequest loginRequest) throws DD4StorageException {
		DD4Protos.User user = userStore.getBy(loginRequest.getUsername(), loginRequest.getPassword());
		if (user != null) {
			requestProvider.get().getSession(true).setAttribute("puser", userStore.updateLastLogin(user));
			return true;
		}
		return false;
	}

	public Empty logout() throws DD4StorageException {
		requestProvider.get().getSession().setAttribute("puser", null);
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
		return !action.equals("login") && !action.equals("logout");
	}
}
