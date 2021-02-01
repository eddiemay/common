package com.digitald4.common.storage.testing;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DAOTestingImpl implements DAO<Message> {
	private AtomicLong idGenerator = new AtomicLong(5000);
	private Map<Class, Map<Long, Message>> tables = new HashMap<>();

	@Override
	public <T extends Message> T create(T t) {
		Map<Long, Message> table = tables.computeIfAbsent(t.getClass(), c -> new HashMap<Long, Message>());
		FieldDescriptor idField = t.getDescriptorForType().findFieldByName("id");
		Long id = (Long) t.getField(idField);
		if (id == 0L) {
			id = idGenerator.incrementAndGet();
			t = (T) t.toBuilder().setField(idField, id).build();
		}
		table.put(id, t);
		return t;
	}

	@Override
	public <T extends Message> T get(Class<T> c, long id) {
		Map<Long, ? extends Message> table = tables.get(c);
		if (table == null) {
			return null;
		}
		return (T) table.get(id);
	}

	@Override
	public <T extends Message> QueryResult<T> list(Class<T> c, Query query) {
		Map<Long, ? extends Message> table = tables.get(c);
		if (table != null) {
			T type = ProtoUtil.getDefaultInstance(c);
			Descriptor descriptor = type.getDescriptorForType();
			List<T> results = table.values().stream().map(item -> (T) item).collect(Collectors.toList());
			for (Filter filter : query.getFilters()) {
				FieldDescriptor field = descriptor.findFieldByName(filter.getColumn());
				results = results.parallelStream()
						.filter(t -> String.valueOf(t.getField(field)).equals(filter.getValue()))
						.collect(Collectors.toList());
			}
			for (OrderBy orderBy : query.getOrderBys()) {
				FieldDescriptor field = descriptor.findFieldByName(orderBy.getColumn());
				results = results.stream()
						.sorted((t1, t2) -> ((Comparable<Object>) t1.getField(field)).compareTo(t2.getField(field)))
						.collect(Collectors.toList());
			}
			if (query.getOffset() > 0) {
				results = results.subList(query.getOffset(), results.size());
			}
			if (query.getLimit() > 0 && results.size() > query.getLimit()) {
				results = results.subList(0, query.getLimit());
			}
			return new QueryResult<>(results, results.size() + query.getOffset());
		}
		return new QueryResult<>(new ArrayList<>(), 0);
	}

	@Override
	public <T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		T t = get(c, id);
		if (t != null) {
			t = updater.apply(t);
			Map<Long, Message> table = tables.get(c);
			table.put(id, t);
		}
		return t;
	}

	@Override
	public <T extends Message> void delete(Class<T> c, long id) {
		Map<Long, ? extends Message> table = tables.get(c);
		if (table != null) {
			table.remove(id);
		}
	}

	@Override
	public <T extends Message> int delete(Class<T> c, Query query) {
		Map<Long, ? extends Message> table = tables.get(c);
		if (table != null) {
			List<T> results = list(c, query).getResults();
			if (results.size() > 0) {
				FieldDescriptor idField = ProtoUtil.getDefaultInstance(c)
						.getDescriptorForType().findFieldByName("id");
				results.parallelStream().forEach(t -> table.remove((Long) t.getField(idField)));
			}
			return results.size();
		}
		return 0;
	}
}
