package com.digitald4.common.server.service;

import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Message;
import org.json.JSONObject;

public interface JSONService {

	JSONObject performAction(String action, JSONObject request);

	boolean requiresLogin(String action);
}
