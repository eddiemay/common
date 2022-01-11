package com.digitald4.common.storage;

import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;

import java.util.function.UnaryOperator;
import javax.inject.Inject;
import javax.inject.Provider;

public class GenericStore<T> implements Store<T> {

	private final Class<T> c;
	private final Provider<DAO> daoProvider;
	private final T type;

	@Inject
	public GenericStore(T type, Provider<DAO> daoProvider) {
		this.c = (Class<T>) type.getClass();
		this.daoProvider = daoProvider;
		this.type = type;
	}

	public GenericStore(Class<T> c, Provider<DAO> daoProvider) {
		this.c = c;
		this.daoProvider = daoProvider;
		this.type = JSONUtil.getDefaultInstance(c);
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
	public ImmutableList<T> create(Iterable<T> entities) {
		return daoProvider.get().create(entities);
	}

	@Override
	public T get(long id) {
		return daoProvider.get().get(c, id);
	}

	@Override
	public ImmutableList<T> get(Iterable<Long> ids) {
		return daoProvider.get().get(c, ids);
	}

	@Override
	public QueryResult<T> list(Query.List query) {
		return daoProvider.get().list(c, query);
	}

	@Override
	public T update(long id, UnaryOperator<T> updater) {
		return daoProvider.get().update(c, id, updater);
	}

	@Override
	public ImmutableList<T> update(Iterable<Long> ids, UnaryOperator<T> updater) {
		return daoProvider.get().update(c, ids, updater);
	}

	@Override
	public void delete(long id) {
		daoProvider.get().delete(c, id);
	}

	@Override
	public void delete(Iterable<Long> ids) {
		daoProvider.get().delete(c, ids);
	}
}
