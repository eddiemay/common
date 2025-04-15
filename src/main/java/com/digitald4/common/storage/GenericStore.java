package com.digitald4.common.storage;
import static com.digitald4.common.util.JSONUtil.copy;
import static com.digitald4.common.util.JSONUtil.getDefaultInstance;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Query.List;
import com.digitald4.common.util.Pair;
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
		return transform(postprocess(daoProvider.get().create(preprocess(t, null))));
	}

	@Override
	public ImmutableList<T> create(Iterable<T> entities) {
		return ImmutableList.copyOf(transform(postprocess(daoProvider.get().create(
				preprocess(stream(entities).map(t -> Pair.of(t, (T) null)).collect(toImmutableList()))))));
	}

	@Override
	public T get(I id) {
		return transform(daoProvider.get().get(c, id));
	}

	@Override
	public BulkGetable.MultiListResult<T, I> get(Iterable<I> ids) {
		return BulkGetable.MultiListResult.of(transform(daoProvider.get().get(c, ids).getItems()), ids);
	}

	@Override
	public QueryResult<T> list(Query.List query) {
		QueryResult<T> result = daoProvider.get().list(c, query);
		transform(result.getItems());
		return result;
	}

	@Override
	public QueryResult<T> search(Query.Search searchQuery) {
		T defaultInstance = getDefaultInstance(c);
		if (!(defaultInstance instanceof Searchable)) {
			throw new DD4StorageException(
					"Unsupported Operation: " + c + " does not implement Searchable", ErrorCode.BAD_REQUEST);
		}

		return (QueryResult<T>) daoProvider.get().search((Class<? extends Searchable>) c, searchQuery);
	}

	@Override
	public T update(I id, UnaryOperator<T> updater) {
		return transform(postprocess(
				daoProvider.get().update(c, id, current -> preprocess(updater.apply(copy(current)), current))));
	}

	@Override
	public ImmutableList<T> update(Iterable<I> ids, UnaryOperator<T> updater) {
		return ImmutableList.copyOf(transform(postprocess(
				daoProvider.get().update(c, ids, current -> preprocess(updater.apply(copy(current)), current)))));
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

	protected T preprocess(T t, T current) {
		return preprocess(ImmutableList.of(Pair.of(t, current))).iterator().next();
	}

	protected Iterable<T> preprocess(Iterable<Pair<T, T>> entities) {
		return stream(entities).map(Pair::getLeft).collect(toImmutableList());
	}

	private T transform(T t) {
		return t == null ? null : transform(ImmutableList.of(t)).iterator().next();
	}

	@Override
	public int index(Iterable<T> items) {
		return daoProvider.get().index(c, items);
	}

	@Override
	public int index(List query) {
		return index(list(query).getItems());
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
