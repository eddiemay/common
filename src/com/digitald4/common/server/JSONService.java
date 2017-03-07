package com.digitald4.common.server;

import com.digitald4.common.exception.DD4StorageException;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by eddiemay on 9/24/16.
 */
public interface JSONService {

	/* JSONObject create(JSONObject request) throws DD4StorageException;

	JSONObject get(JSONObject request) throws DD4StorageException;

	JSONArray list(JSONObject request) throws DD4StorageException;

	JSONObject update(JSONObject request) throws DD4StorageException;

	boolean delete(JSONObject request) throws DD4StorageException; */

	Object performAction(String action, JSONObject request) throws Exception;

	boolean requiresLogin(String action);

	public static <R extends Message> R transformJSONRequest(R msgRequest, HttpServletRequest request)
			throws JsonFormat.ParseException, JSONException {
		return transformJSONRequest(msgRequest, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
	}

	@SuppressWarnings("unchecked")
	public static <R extends Message> R transformJSONRequest(R msgRequest, JSONObject json) throws JsonFormat.ParseException {
		R.Builder builder = msgRequest.toBuilder();
		JsonFormat.merge(json.toString(), builder);
		return (R) builder.build();
	}

	public static JSONArray convertToJSON(List<? extends Message> items) throws JSONException {
		JSONArray array = new JSONArray();
		for (Message item : items) {
			array.put(convertToJSON(item));
		}
		return array;
	}

	public static JSONObject convertToJSON(Message item) {
		try {
			return new JSONObject(JsonFormat.printToString(item));
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public static JSONObject convertToJSON(boolean bool) throws JSONException {
		return new JSONObject(bool);
	}
}
