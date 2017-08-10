package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import org.json.JSONObject;

public interface JSONService {

	JSONObject create(JSONObject request) throws DD4StorageException;

	JSONObject get(JSONObject request) throws DD4StorageException;

	JSONObject list(JSONObject request) throws DD4StorageException;

	JSONObject update(JSONObject request) throws DD4StorageException;

	JSONObject delete(JSONObject request) throws DD4StorageException;

	JSONObject performAction(String action, JSONObject request) throws Exception;

	boolean requiresLogin(String action);
}
