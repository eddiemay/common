package com.digitald4.common.storage;

import java.util.function.UnaryOperator;
import javax.inject.Provider;

public class GenericStore<T> implements Store<T> {

	private final Class<T> c;
	private final Provider<DAO> daoProvider;
	private final T type;

	public GenericStore(Class<T> c, Provider<DAO> daoProvider) {
		this.c = c;
		this.daoProvider = daoProvider;
		this.type = DAORouterImpl.getDefaultInstance(c);
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
