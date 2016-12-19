package com.digitald4.common.server;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by eddiemay on 9/24/16.
 */
public interface JSONService {
	JSONObject performAction(String action, String json) throws Exception;

	public static <R extends Message> R transformJSONRequest(R msgRequest, HttpServletRequest request) throws JsonFormat.ParseException {
		return transformJSONRequest(msgRequest, request.getParameterMap().values().iterator().next()[0]);
	}

	@SuppressWarnings("unchecked")
	public static <R extends Message> R transformJSONRequest(R msgRequest, String json) throws JsonFormat.ParseException {
		R.Builder builder = msgRequest.toBuilder();
		JsonFormat.merge(json, builder);
		return (R) builder.build();
	}

	public static JSONArray convertToJSON(List<? extends Message> items) throws JSONException {
		JSONArray array = new JSONArray();
		for (Message item : items) {
			array.put(convertToJSON(item));
		}
		return array;
	}

	public static JSONObject convertToJSON(Message item) throws JSONException {
		return new JSONObject(JsonFormat.printToString(item));
	}

	public static JSONObject convertToJSON(boolean bool) throws JSONException {
		return new JSONObject(bool);
	}
}