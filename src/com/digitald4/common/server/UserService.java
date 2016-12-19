package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.LoginRequest;
import com.digitald4.common.proto.DD4UIProtos.UserUI;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.Provider;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by eddiemay on 9/24/16.
 */
public class UserService extends DualProtoService<UserUI, User> {

	private final UserStore userStore;
	private final Provider<HttpServletRequest> requestProvider;

	public UserService(UserStore userStore, Provider<HttpServletRequest> requestProvider) {
		super(UserUI.class, userStore);
		this.userStore = userStore;
		this.requestProvider = requestProvider;
	}

	@Override
	public JSONObject performAction(String action, String jsonRequest)
			throws DD4StorageException, JSONException, ParseException {
		JSONObject json = new JSONObject();
		if (action.equals("login")) {
			json.put("data", JSONService.convertToJSON(
					login(JSONService.transformJSONRequest(LoginRequest.getDefaultInstance(), jsonRequest))));
		} else {
			return super.performAction(action, jsonRequest);
		}
		return json;
	}

	public boolean login(LoginRequest loginRequest) throws DD4StorageException {
		User user = userStore.getBy(loginRequest.getUsername(), loginRequest.getPassword());
		if (user != null) {
			requestProvider.get().getSession(true).setAttribute("puser", userStore.updateLastLogin(user));
			return true;
		}
		return false;
	}
}
