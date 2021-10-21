package com.digitald4.common.server.service;

import com.google.api.server.spi.ServiceException;
import org.json.JSONObject;

public interface JSONService {
	JSONObject performAction(String action, JSONObject request) throws ServiceException;
}
