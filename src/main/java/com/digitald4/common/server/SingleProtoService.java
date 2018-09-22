package com.digitald4.common.server;

import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.google.protobuf.GeneratedMessageV3;
import java.util.function.UnaryOperator;

public class SingleProtoService<T extends GeneratedMessageV3> extends DualProtoService<T, T> {

	private final UnaryOperator<T> converter = t -> t;

	public SingleProtoService(Store<T> store) {
		super(store.getType(), store);
	}

	@Override
	protected QueryResult<T> toListResponse(QueryResult<T> results) {
		return results;
	}

	@Override
	protected UnaryOperator<T> getConverter() {
		return converter;
	}

	@Override
	protected UnaryOperator<T> getReverseConverter() {
		return converter;
	}
}
