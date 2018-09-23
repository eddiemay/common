package com.digitald4.common.server;

import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.json.JSONObject;

public class UpdateRequest<T extends Message> {
	private final T entity;
	private final FieldMask updateMask;

	public UpdateRequest(T entity, FieldMask updateMask) {
		this.entity = entity;
		this.updateMask = updateMask;
	}

	public T getEntity() {
		return entity;
	}

	public FieldMask getUpdateMask() {
		return updateMask;
	}

	public JSONObject toJSON() {
		return 	new JSONObject()
				.put("entity", ProtoUtil.toJSON(entity))
				.put("updateMask", ProtoUtil.toJSON(updateMask));
	}
}
