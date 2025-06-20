package com.digitald4.common.storage;

import static com.google.common.collect.Streams.stream;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.model.Searchable;
import com.digitald4.common.server.service.BulkGetable;
import com.digitald4.common.storage.Transaction.Op;
import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;

public interface TypedDAO<R> {
	<T extends R> Transaction<T> persist(Transaction<T> transaction);

	default <T extends R> T create(T t) {
		return persist(Transaction.of(Op.create(t))).getOps().get(0).getEntity();
	}

	default <T extends R> ImmutableList<T> create(Iterable<T> ts, UnaryOperator<T> updater) {
		return persist(Transaction.of(stream(ts).map(Op::create).collect(toImmutableList())))
				.getOps().stream().map(Op::getEntity).collect(toImmutableList());
	}

	<T extends R, I> T get(Class<T> c, I id);

	<T extends R, I> BulkGetable.MultiListResult<T, I> get(Class<T> c, Iterable<I> ids);

	<T extends R> QueryResult<T> list(Class<T> c, Query.List listQuery);

	<T extends Searchable> QueryResult<T> search(Class<T> c, Query.Search searchQuery);

	default <T extends R, I> T update(Class<T> c, I id, UnaryOperator<T> updater) {
		return persist(Transaction.of(Op.update(c, id, updater))).getOps().get(0).getEntity();
	}

	default <T extends R, I> ImmutableList<T> update(Class<T> c, Iterable<I> ids, UnaryOperator<T> updater) {
		return persist(Transaction.of(stream(ids).map(id -> Op.update(c, id, updater)).collect(toImmutableList())))
				.getOps().stream().map(Op::getEntity).collect(toImmutableList());
	}

	<T extends R, I> boolean delete(Class<T> c, I id);

	<T extends R, I> int delete(Class<T> c, Iterable<I> ids);

	default <T extends R, I> int index(Class<T> c, Iterable<T> items) {
		throw new IllegalArgumentException("Index not implemented for DAO");
	}
}
