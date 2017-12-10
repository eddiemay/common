package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import org.json.JSONObject;

public interface JSONService {

	JSONObject create(JSONObject request);

	JSONObject get(JSONObject request);

	JSONObject list(JSONObject request);

	JSONObject update(JSONObject request);

	JSONObject delete(JSONObject request);

	JSONObject performAction(String action, JSONObject request) throws Exception;

	boolean requiresLogin(String action);
}
