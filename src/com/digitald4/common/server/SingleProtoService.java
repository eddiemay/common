package com.digitald4.common.server;

import com.digitald4.common.storage.Store;
import com.google.protobuf.GeneratedMessage;

import java.util.function.UnaryOperator;

public class SingleProtoService<T extends GeneratedMessage> extends DualProtoService<T, T> {

	private final UnaryOperator<T> converter = new UnaryOperator<T>() {
		@Override
		public T apply(T t) {
			return t;
		}
	};

	public SingleProtoService(Store<T> store) {
		super(store.getType(), store);
	}

	@Override
	public UnaryOperator<T> getConverter() {
		return converter;
	}

	@Override
	public UnaryOperator<T> getReverseConverter() {
		return converter;
	}
}
