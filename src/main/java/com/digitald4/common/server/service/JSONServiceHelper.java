package com.digitald4.common.server.service;

import static com.digitald4.common.util.JSONUtil.toJSON;
import static com.digitald4.common.util.JSONUtil.toObject;

import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.response.BadRequestException;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONServiceHelper<T> implements JSONService {
	private static final BadRequestException BAD_REQUEST = new BadRequestException("Invalid action");
	private final Class<T> cls;
	private final EntityService<T> entityService;

	public JSONServiceHelper(EntityService<T> entityService) {
		this.cls = entityService.getTypeClass();
		this.entityService = entityService;
	}

	@Override
	public JSONObject performAction(String action, JSONObject jsonRequest) throws ServiceException {
		switch (action) {
			case "create": {
				if (entityService instanceof Createable) {
					return toJSON(((Createable<T>) entityService).create(
							toObject(cls, jsonRequest.getJSONObject("entity")), jsonRequest.optString("idToken")));
				}
				throw BAD_REQUEST;
			}
			case "get": {
				if (entityService instanceof Getable) {
					return toJSON(((Getable<?,Long>) entityService).get(
							jsonRequest.getLong("id"), jsonRequest.optString("idToken")));
				}
				throw BAD_REQUEST;
			}
			case "list": {
				if (entityService instanceof Listable) {
					return toJSON(
							((Listable<?>) entityService).list(
									jsonRequest.optString("fields"),
									jsonRequest.optString("filter"), jsonRequest.optString("orderBy"),
									jsonRequest.optInt("pageSize"), jsonRequest.optInt("pageToken"),
									jsonRequest.optString("idToken")));
				}
				throw BAD_REQUEST;
			}
			case "update": {
				if (entityService instanceof Updateable) {
					return toJSON(
							((Updateable<T, Long>) entityService).update(
									jsonRequest.getLong("id"),
									toObject(cls, jsonRequest.getJSONObject("entity")),
									jsonRequest.getString("updateMask"), jsonRequest.optString("idToken")));
				}
				throw BAD_REQUEST;
			}
			case "delete": {
				if (entityService instanceof Deleteable) {
					return toJSON(((Deleteable) entityService).delete(
							jsonRequest.getInt("id"), jsonRequest.optString("idToken")));
				}
				throw BAD_REQUEST;
			}
			case "batchDelete": {
				if (entityService instanceof BulkDeleteable) {
					JSONArray ids = jsonRequest.getJSONArray("ids");
					/*return toJSON(
							((BulkDeleteable) entityService).batchDelete(
									JSONUtil.transform(ids, ids::getLong), jsonRequest.optString("idToken")));*/
				}
				throw BAD_REQUEST;
			}
			default:
				throw BAD_REQUEST;
		}
	}
}
