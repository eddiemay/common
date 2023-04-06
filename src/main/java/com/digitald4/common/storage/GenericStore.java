package com.digitald4.common.storage;

import com.google.common.collect.ImmutableList;

import java.util.function.UnaryOperator;
import javax.inject.Inject;
import javax.inject.Provider;

public class GenericStore<T, I> implements Store<T, I> {

	private final Class<T> c;
	private final Provider<DAO> daoProvider;

	@Inject
	public GenericStore(T type, Provider<DAO> daoProvider) {
		this.c = (Class<T>) type.getClass();
		this.daoProvider = daoProvider;
	}

	public GenericStore(Class<T> c, Provider<DAO> daoProvider) {
		this.c = c;
		this.daoProvider = daoProvider;
	}

	@Override
	public Class<T> getTypeClass() {
		return c;
	}

	@Override
	public T create(T t) {
		return postprocess(daoProvider.get().create(preprocess(t, true)));
	}

	@Override
	public ImmutableList<T> create(Iterable<T> entities) {
		return postprocess(daoProvider.get().create(preprocess(entities, true)));
	}

	@Override
	public T get(I id) {
		return daoProvider.get().get(c, id);
	}

	@Override
	public ImmutableList<T> get(Iterable<I> ids) {
		return daoProvider.get().get(c, ids);
	}

	@Override
	public QueryResult<T> list(Query.List query) {
		return daoProvider.get().list(c, query);
	}

	@Override
	public T update(I id, UnaryOperator<T> updater) {
		return postprocess(
				daoProvider.get().update(c, id, current -> preprocess(updater.apply(current), false)));
	}

	@Override
	public ImmutableList<T> update(Iterable<I> ids, UnaryOperator<T> updater) {
		return postprocess(
				daoProvider.get().update(c, ids, current -> preprocess(updater.apply(current), false)));
	}

	@Override
	public void delete(I id) {
		daoProvider.get().delete(c, id);
		postdelete(ImmutableList.of(id));
	}

	@Override
	public void delete(Iterable<I> ids) {
		daoProvider.get().delete(c, ids);
		postdelete(ids);
	}

	private T preprocess(T t, boolean isCreate) {
		return preprocess(ImmutableList.of(t), isCreate).iterator().next();
	}

	protected Iterable<T> preprocess(Iterable<T> entities, boolean isCreate) {
		return entities;
	}

	private T postprocess(T t) {
		return postprocess(ImmutableList.of(t)).get(0);
	}

	protected ImmutableList<T> postprocess(ImmutableList<T> entities) {
		return entities;
	}

	protected void postdelete(Iterable<I> ids) {}
}
