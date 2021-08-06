package com.digitald4.common.server.service;

import static com.digitald4.common.util.JSONUtil.toJSON;
import static com.digitald4.common.util.JSONUtil.toObject;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.Store;
import com.digitald4.common.util.JSONUtil;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONServiceHelper<T> implements JSONService {
	private static final DD4StorageException BAD_REQUEST =
			new DD4StorageException("Invalid action", HttpServletResponse.SC_BAD_REQUEST);
	private final Class<T> cls;
	private final EntityService<T> entityService;
	private final boolean requiresLoginDefault;

	public JSONServiceHelper(EntityService<T> entityService, boolean requiresLoginDefault) {
		Store<T> store = entityService.getStore();
		if (entityService instanceof DualProtoService) {
			this.cls = (Class<T>) ((DualProtoService<?, ?>) entityService).getType().getClass();
		} else {
			this.cls = (Class<T>) store.getType().getClass();
		}
		this.entityService = entityService;
		this.requiresLoginDefault = requiresLoginDefault;
	}

	@Override
	public boolean requiresLogin(String action) {
		return requiresLoginDefault;
	}

	@Override
	public JSONObject performAction(String action, JSONObject jsonRequest) {
		switch (action) {
			case "create": {
				if (entityService instanceof Createable) {
					return toJSON(((Createable<T>) entityService).create(toObject(cls, jsonRequest.getJSONObject("entity"))));
				}
				throw BAD_REQUEST;
			}
			case "get": {
				if (entityService instanceof Getable) {
					return toJSON(((Getable<?>) entityService).get(jsonRequest.getInt("id")));
				}
				throw BAD_REQUEST;
			}
			case "list": {
				if (entityService instanceof Listable) {
					return toJSON(
							((Listable<?>) entityService).list(
									jsonRequest.optString("filter"), jsonRequest.optString("orderBy"),
									jsonRequest.optInt("pageSize"), jsonRequest.optInt("pageToken")));
				}
				throw BAD_REQUEST;
			}
			case "update": {
				if (entityService instanceof Updateable) {
					return toJSON(
							((Updateable<T>) entityService).update(
									jsonRequest.getLong("id"),
									toObject(cls, jsonRequest.getJSONObject("entity")),
									jsonRequest.getString("updateMask")));
				}
				throw BAD_REQUEST;
			}
			case "delete": {
				if (entityService instanceof Deleteable) {
					return toJSON(((Deleteable) entityService).delete(jsonRequest.getInt("id")));
				}
				throw BAD_REQUEST;
			}
			case "batchDelete": {
				if (entityService instanceof BulkDeleteable) {
					JSONArray ids = jsonRequest.getJSONArray("ids");
					return toJSON(((BulkDeleteable) entityService).batchDelete(JSONUtil.transform(ids, ids::getLong)));
				}
				throw BAD_REQUEST;
			}
			default:
				throw BAD_REQUEST;
		}
	}
}
