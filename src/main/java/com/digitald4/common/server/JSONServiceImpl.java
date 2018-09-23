package com.digitald4.common.server;

import static com.digitald4.common.util.ProtoUtil.toJSON;
import static com.digitald4.common.util.ProtoUtil.toProto;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class  JSONServiceImpl<T extends Message> implements JSONService {
	private final T type;
	private final ProtoService<T> protoService;
	private final boolean requiresLoginDefault;

	public JSONServiceImpl(DualProtoService<T, ?> protoService, boolean requiresLoginDefault) {
		this.type = protoService.getType();
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
			case "create":
				return toJSON(protoService.create(toProto(type, jsonRequest)));
			case "get":
				return toJSON(protoService.get(jsonRequest.getInt("id")));
			case "list":
				return toJSON(protoService.list(toProto(ListRequest.getDefaultInstance(), jsonRequest)));
			case "update":
				return toJSON(protoService.update(
						jsonRequest.getLong("id"),
						new UpdateRequest<>(
								toProto(type, jsonRequest.getJSONObject("entity")),
								FieldMask.newBuilder().addPaths(jsonRequest.getString("updateMask")).build())));
			case "delete":
				return toJSON(protoService.delete(jsonRequest.getInt("id")));
			case "batchDelete":
				return toJSON(protoService.batchDelete(toProto(BatchDeleteRequest.getDefaultInstance(), jsonRequest)));
			default:
				throw new DD4StorageException("Invalid action: " + action, HttpServletResponse.SC_BAD_REQUEST);
		}
	}
}
