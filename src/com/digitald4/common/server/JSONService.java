package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface JSONService {

	JSONObject create(JSONObject request) throws DD4StorageException;

	JSONObject get(JSONObject request) throws DD4StorageException;

	JSONArray list(JSONObject request) throws DD4StorageException;

	JSONObject update(JSONObject request) throws DD4StorageException;

	boolean delete(JSONObject request) throws DD4StorageException;

	Object performAction(String action, JSONObject request) throws Exception;

	boolean requiresLogin(String action);
}
