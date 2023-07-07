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
		return transform(postprocess(daoProvider.get().create(preprocess(t, true))));
	}

	@Override
	public ImmutableList<T> create(Iterable<T> entities) {
		return ImmutableList.copyOf(transform(postprocess(daoProvider.get().create(preprocess(entities, true)))));
	}

	@Override
	public T get(I id) {
		return transform(daoProvider.get().get(c, id));
	}

	@Override
	public ImmutableList<T> get(Iterable<I> ids) {
		return ImmutableList.copyOf(transform(daoProvider.get().get(c, ids)));
	}

	@Override
	public QueryResult<T> list(Query.List query) {
		QueryResult<T> result = daoProvider.get().list(c, query);
		transform(result.getItems());
		return result;
	}

	@Override
	public T update(I id, UnaryOperator<T> updater) {
		return transform(postprocess(
				daoProvider.get().update(c, id, current -> preprocess(updater.apply(current), false))));
	}

	@Override
	public ImmutableList<T> update(Iterable<I> ids, UnaryOperator<T> updater) {
		return ImmutableList.copyOf(transform(postprocess(
				daoProvider.get().update(c, ids, current -> preprocess(updater.apply(current), false)))));
	}

	@Override
	public boolean delete(I id) {
		boolean result = daoProvider.get().delete(c, id);
		postdelete(ImmutableList.of(id));
		return result;
	}

	@Override
	public int delete(Iterable<I> ids) {
		int result = daoProvider.get().delete(c, ids);
		postdelete(ids);
		return result;
	}

	protected T preprocess(T t, boolean isCreate) {
		return preprocess(ImmutableList.of(t), isCreate).iterator().next();
	}

	protected Iterable<T> preprocess(Iterable<T> entities, boolean isCreate) {
		return entities;
	}

	private T transform(T t) {
		return transform(ImmutableList.of(t)).iterator().next();
	}

	protected Iterable<T> transform(Iterable<T> entities) {
		return entities;
	}

	private T postprocess(T t) {
		return postprocess(ImmutableList.of(t)).iterator().next();
	}

	protected Iterable<T> postprocess(Iterable<T> entities) {
		return entities;
	}

	protected void postdelete(Iterable<I> ids) {}
}
