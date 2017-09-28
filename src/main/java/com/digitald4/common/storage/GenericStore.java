package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.util.Provider;
import com.google.protobuf.GeneratedMessageV3;
import java.lang.reflect.InvocationTargetException;
import java.util.function.UnaryOperator;

public class GenericStore<T extends GeneratedMessageV3> implements Store<T> {

	private final Class<T> c;
	private final T type;
	private final Provider<DAO> dataAccessObjectProvider;
	
	public GenericStore(Class<T> c, Provider<DAO> dataAccessObjectProvider) {
		try {
			this.c = c;
			this.type = (T) c.getMethod("getDefaultInstance").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		this.dataAccessObjectProvider = dataAccessObjectProvider;
	}
	
	@Override
	public T getType() {
		return type;
	}
	
	@Override
	public T create(T t) throws DD4StorageException {
		return dataAccessObjectProvider.get().create(t);
	}

	@Override
	public T get(long id) throws DD4StorageException {
		return dataAccessObjectProvider.get().get(c, id);
	}

	@Override
	public QueryResult<T> list(Query query) throws DD4StorageException {
		return dataAccessObjectProvider.get().list(c, query);
	}

	@Override
	public T update(long id, UnaryOperator<T> updater) throws DD4StorageException {
		return dataAccessObjectProvider.get().update(c, id, updater);
	}

	@Override
	public void delete(long id) throws DD4StorageException {
		dataAccessObjectProvider.get().delete(c, id);
	}
}
