package com.digitald4.common.model;

import com.google.protobuf.Message;

public interface HasProto<T extends Message> {
	T toProto();

	HasProto<T> fromProto(T proto);
}
