package com.digitald4.common.server.service;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.digitald4.common.util.ProtoUtil.toJSON;
import static com.digitald4.common.util.ProtoUtil.toProto;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.ProtoStore;
import com.digitald4.common.storage.Store;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.IntStream;

public class JSONServiceImpl<T extends Message> implements JSONService {
	private static final DD4StorageException BAD_REQUEST =
			new DD4StorageException("Invalid action", HttpServletResponse.SC_BAD_REQUEST);
	private final T type;
	private final EntityService<T> protoService;
	private final boolean requiresLoginDefault;

	public JSONServiceImpl(EntityService<T> protoService, boolean requiresLoginDefault) {
		Store<T> store = protoService.getStore();
		if (protoService instanceof DualProtoService) {
			this.type = ((DualProtoService<T, ?>) protoService).getType();
		} else if (store instanceof ProtoStore) {
			this.type = ((ProtoStore<T>) store).getType();
		} else {
			throw new IllegalArgumentException("Unable to determine proto type");
		}
		this.protoService = protoService;
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
				if (protoService instanceof Createable) {
					return toJSON(((Createable<T>) protoService).create(toProto(type, jsonRequest)));
				}
				throw BAD_REQUEST;
			}
			case "get": {
				if (protoService instanceof Getable) {
					return toJSON(((Getable<?>) protoService).get(jsonRequest.getInt("id")));
				}
				throw BAD_REQUEST;
			}
			case "list": {
				if (protoService instanceof Listable) {
					return toJSON(
							((Listable<?>) protoService).list(
									jsonRequest.optString("filter"), jsonRequest.optString("orderBy"),
									jsonRequest.optInt("pageSize"), jsonRequest.optInt("pageToken")));
				}
				throw BAD_REQUEST;
			}
			case "update": {
				if (protoService instanceof Updateable) {
					return toJSON(
							((Updateable<T>) protoService).update(
									jsonRequest.getLong("id"),
									new UpdateRequest<>(
											toProto(type, jsonRequest.getJSONObject("entity")),
											getStringArray(jsonRequest,"updateMask"))));
				}
				throw BAD_REQUEST;
			}
			case "delete": {
				if (protoService instanceof Deleteable) {
					return toJSON(((Deleteable) protoService).delete(jsonRequest.getInt("id")));
				}
				throw BAD_REQUEST;
			}
			case "batchDelete": {
				if (protoService instanceof BulkDeleteable) {
					return toJSON(
							((BulkDeleteable) protoService).batchDelete(
									jsonRequest.optString("filter"), jsonRequest.optString("orderBy"),
									jsonRequest.optInt("pageSize"), jsonRequest.optInt("pageToken")));
				}
				throw BAD_REQUEST;
			}
			default:
				throw BAD_REQUEST;
		}
	}

	public static ImmutableList<String> getStringArray(JSONObject json, String key) {
		JSONArray jsonArray = json.optJSONArray(key);
		if (jsonArray == null) {
			return ImmutableList.of();
		}

		return IntStream.range(0, jsonArray.length())
				.mapToObj(jsonArray::getString)
				.collect(toImmutableList());

	}
}
