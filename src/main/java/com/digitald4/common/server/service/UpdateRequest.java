package com.digitald4.common.server.service;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.FieldMask;

public class UpdateRequest<T> {
	private final T entity;
	private final ImmutableList<String> updateMask;

	public UpdateRequest(T entity, Iterable<String> updateMask) {
		this.entity = entity;
		this.updateMask = ImmutableList.copyOf(updateMask);
	}

	public T getEntity() {
		return entity;
	}

	public ImmutableList<String> getUpdateMask() {
		return updateMask;
	}

	public FieldMask updateMask() {
		return FieldMask.newBuilder().addAllPaths(updateMask).build();
	}
}
