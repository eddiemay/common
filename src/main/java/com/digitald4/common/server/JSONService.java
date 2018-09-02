package com.digitald4.common.server;

import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Message;
import org.json.JSONObject;

public interface JSONService {

	JSONObject performAction(String action, JSONObject request) throws Exception;

	boolean requiresLogin(String action);

	static <R extends Message> R toProto(R msgRequest, JSONObject json) {
		R.Builder builder = msgRequest.toBuilder();
		ProtoUtil.merge(json, builder);
		return (R) builder.build();
	}
}
