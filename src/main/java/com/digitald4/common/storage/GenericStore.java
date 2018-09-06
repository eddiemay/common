package com.digitald4.common.storage;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Message;
import java.util.function.UnaryOperator;
import javax.inject.Inject;
import javax.inject.Provider;

public class GenericStore<T extends Message> implements Store<T> {

	private final Class<T> c;
	private final T type;
	private final Provider<DAO> daoProvider;

	@Inject
	public GenericStore(Class<T> c, Provider<DAO> daoProvider) {
		this.c = c;
		this.type = ProtoUtil.getDefaultInstance(c);
		this.daoProvider = daoProvider;
	}
	
	@Override
	public T getType() {
		return type;
	}
	
	@Override
	public T create(T t) {
		return daoProvider.get().create(t);
	}

	@Override
	public T get(long id) {
		return daoProvider.get().get(c, id);
	}

	@Override
	public QueryResult<T> list(Query query) {
		return daoProvider.get().list(c, query);
	}

	@Override
	public T update(long id, UnaryOperator<T> updater) {
		return daoProvider.get().update(c, id, updater);
	}

	@Override
	public void delete(long id) {
		daoProvider.get().delete(c, id);
	}

	@Override
	public int delete(Query query) {
		return daoProvider.get().delete(c, query);
	}
}
