package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.util.Provider;
import com.google.protobuf.GeneratedMessageV3;
import java.lang.reflect.InvocationTargetException;
import java.util.function.UnaryOperator;

public class DAOConnectorImpl<T extends GeneratedMessageV3> implements DAO<T> {

	private final Class<T> cls;
	private final T type;
	private final Provider<DataConnector> dataConnectorProvider;

	public DAOConnectorImpl(Class<T> cls, Provider<DataConnector> dataConnectorProvider) {
		this.cls = cls;
		try {
			this.type = (T) cls.getMethod("getDefaultInstance").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		this.dataConnectorProvider = dataConnectorProvider;
	}

	public T getType() {
		return type;
	}

	public T create(T t) {
		return dataConnectorProvider.get().create(t);
	}

	public T get(int id) {
		return dataConnectorProvider.get().get(cls, id);
	}

	public ListResponse<T> list(ListRequest listRequest) {
		return dataConnectorProvider.get().list(cls, listRequest);
	}

	public T update(int id, UnaryOperator<T> updater) {
		return dataConnectorProvider.get().update(cls, id, updater);
	}

	public void delete(int id) {
		dataConnectorProvider.get().delete(cls, id);
	}
}
