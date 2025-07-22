package com.digitald4.common.storage;
import static com.digitald4.common.util.JSONUtil.getDefaultInstance;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.HasModificationTimes;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.model.SoftDeletable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Query.List;
import com.digitald4.common.storage.Transaction.Op;
import com.google.common.collect.ImmutableList;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
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
		Op<T> op = Op.create(t, this::preprocess, this::postprocess);
		daoProvider.get().persist(Transaction.of(op, this::preprocess, this::postprocess));
		return op.getEntity();
	}

	@Override
	public ImmutableList<T> create(Iterable<T> entities) {
		ImmutableList<Op<T>> ops = stream(entities)
				.map(t -> Op.create(t, this::preprocess, this::postprocess))
				.collect(toImmutableList());
		daoProvider.get().persist(Transaction.of(ops, this::preprocess, this::postprocess));
		return ops.stream().map(Op::getEntity).collect(toImmutableList());
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
		Op<T> op = Op.update(c, id, updater, this::preprocess, this::postprocess);
		daoProvider.get().persist(Transaction.of(op, this::preprocess, this::postprocess));
		return op.getEntity();
	}

	@Override
	public ImmutableList<T> update(Iterable<I> ids, UnaryOperator<T> updater) {
		ImmutableList<Op<T>> ops = stream(ids)
				.map(id -> Op.update(c, id, updater, this::preprocess, this::postprocess))
				.collect(toImmutableList());
		daoProvider.get().persist(Transaction.of(ops, this::preprocess, this::postprocess));
		return ops.stream().map(Op::getEntity).collect(toImmutableList());
	}

	@Override
	public ImmutableList<T> migrate(Iterable<T> entities) {
		ImmutableList<Op<T>> ops = stream(entities)
				.map(t -> Op.migrate(t, this::preprocess, this::postprocess))
				.collect(toImmutableList());
		daoProvider.get().persist(Transaction.of(ops, this::preprocess, this::postprocess));
		return ops.stream().map(Op::getEntity).collect(toImmutableList());
	}

	@Override
	public boolean delete(I id) {
		return delete(ImmutableList.of(id)) == 1;
	}

	@Override
	public int delete(Iterable<I> ids) {
		int result = 0;
		T defaultInstance = getDefaultInstance(getTypeClass());
		if (defaultInstance instanceof SoftDeletable<?>) {
			ImmutableSet<I> softDeleteIds = get(ids).getItems().stream()
					.map(e -> (SoftDeletable<I>) e)
					.filter(mo -> mo.getDeletionTime() == null)
					.map(HasModificationTimes::getId)
					.collect(toImmutableSet());
			ImmutableSet<I> hardDeleteIds =
					stream(ids).filter(id -> !softDeleteIds.contains(id)).collect(toImmutableSet());
			update(softDeleteIds, current -> (T) ((SoftDeletable<?>) current)
					.setDeletionTime(Instant.now()));
			result += softDeleteIds.size() + daoProvider.get().delete(c, hardDeleteIds);
		} else {
			result = daoProvider.get().delete(c, ids);
		}

		postdelete(ids);
		return result;
	}

	protected Op<T> preprocess(Op<T> op) {
		return op;
	}

	protected Iterable<Op<T>> preprocess(Iterable<Op<T>> ops) {
		return ops;
	}

	protected Op<T> postprocess(Op<T> op) {
		return op;
	}

	protected Iterable<Op<T>> postprocess(Iterable<Op<T>> ops) {
		return ops;
	}

	private T transformOp(Op<T> op) {
		return transform(op.getEntity());
	}

	private T transform(T t) {
		return t == null ? null : transform(ImmutableList.of(t)).iterator().next();
	}

	protected Iterable<T> transformOps(Iterable<Op<T>> ops) {
		return transform(stream(ops).map(Op::getEntity).collect(toImmutableList()));
	}

	protected Iterable<T> transform(Iterable<T> entities) {
		return entities;
	}

	protected void postdelete(Iterable<I> ids) {}

	@Override
	public int index(Iterable<T> items) {
		return daoProvider.get().index(c, items);
	}

	@Override
	public int index(List query) {
		return index(list(query).getItems());
	}
}
