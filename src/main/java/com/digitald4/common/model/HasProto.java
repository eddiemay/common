package com.digitald4.common.model;

import com.google.protobuf.Message;

public interface HasProto<T extends Message> {
	public T getProto();

	public HasProto<T> setProto(T proto);

	public T toProto();
}
