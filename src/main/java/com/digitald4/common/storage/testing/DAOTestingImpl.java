package com.digitald4.common.storage.testing;

import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.Query.OrderBy;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.QueryResult;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DAOTestingImpl implements DAO {
	private AtomicLong idGenerator = new AtomicLong(5000);
	private Map<Class, Map<Long, GeneratedMessageV3>> tables = new HashMap<>();

	@Override
	public <T extends GeneratedMessageV3> T create(T t) {
		Map<Long, GeneratedMessageV3> table = tables.computeIfAbsent(t.getClass(), c -> new HashMap<Long, GeneratedMessageV3>());
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
	public <T extends GeneratedMessageV3> T get(Class<T> c, long id) {
		Map<Long, ? extends GeneratedMessageV3> table = tables.get(c);
		if (table == null) {
			return null;
		}
		return (T) table.get(id);
	}

	@Override
	public <T extends GeneratedMessageV3> QueryResult<T> list(Class<T> c, Query query) {
		QueryResult.Builder<T> result = QueryResult.newBuilder();
		Map<Long, ? extends GeneratedMessageV3> table = tables.get(c);
		if (table != null) {
			T type = DAOCloudDS.getDefaultInstance(c);
			Descriptor descriptor = type.getDescriptorForType();
			List<T> results = (List<T>) table.values().stream().collect(Collectors.toList());
			for (Filter filter : query.getFilterList()) {
				FieldDescriptor field = descriptor.findFieldByName(filter.getColumn());
				results = results.parallelStream()
						.filter(t -> String.valueOf(t.getField(field)).equals(filter.getValue()))
						.collect(Collectors.toList());
			}
			for (OrderBy orderBy : query.getOrderByList()) {
				FieldDescriptor field = descriptor.findFieldByName(orderBy.getColumn());
				results = results.stream()
						.sorted((t1, t2) -> ((Comparable<Object>) t1.getField(field)).compareTo(t2.getField(field)))
						.collect(Collectors.toList());
			}
			if (query.getOffset() > 0) {
				results = results.subList(query.getOffset(), results.size());
			}
			result.setTotalSize(results.size() + query.getOffset());
			if (query.getLimit() > 0 && results.size() > query.getLimit()) {
				results = results.subList(0, query.getLimit());
			}
			result.setResultList(results);
		}
		return result.build();
	}

	@Override
	public <T extends GeneratedMessageV3> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		T t = get(c, id);
		if (t != null) {
			t = updater.apply(t);
			Map<Long, GeneratedMessageV3> table = tables.get(c);
			table.put(id, t);
		}
		return t;
	}

	@Override
	public <T> void delete(Class<T> c, long id) {
		Map<Long, ? extends GeneratedMessageV3> table = tables.get(c);
		if (table != null) {
			table.remove(id);
		}
	}
}
