package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4UIProtos.CreateRequest;
import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.proto.DD4UIProtos.UpdateRequest;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.RetryableFunction;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DAOAPIImpl implements DAO {
	private static final String API_PAYLOAD = "json=%s";
	private final APIConnector apiConnector;
	private final Parser jsonParser;

	public DAOAPIImpl(APIConnector apiConnector) {
		this.apiConnector = apiConnector;
		jsonParser = JsonFormat.parser();
	}

	@Override
	public <T extends Message> T create(T t) {
		String url = apiConnector.getApiUrl() + "/" + getResourceName(t.getClass());
		JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(t.getDescriptorForType()).build();
		Printer jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
		Parser jsonParser = JsonFormat.parser().usingTypeRegistry(registry);
		try {
			CreateRequest request = CreateRequest.newBuilder()
					.setEntity(Any.pack(t))
					.build();
			Message.Builder builder = t.newBuilderForType();
			jsonParser.merge(apiConnector.sendPost(url, String.format(API_PAYLOAD, jsonPrinter.print(request))), builder);
			return (T) builder.build();
		} catch (Exception ioe) {
			throw new DD4StorageException(ioe);
		}
	}

	@Override
	public <T extends Message> T get(Class<T> c, long id) {
		String url = apiConnector.getApiUrl() + "/" + getResourceName(c) + "/" + id;
		try {
			String json = apiConnector.sendGet(url);
			Message.Builder builder = DAOCloudDS.getDefaultInstance(c).toBuilder();
			jsonParser.merge(json, builder);
			return (T) builder.build();
		} catch (IOException ioe) {
			throw new DD4StorageException(ioe);
		}
	}

	@Override
	public <T extends Message> QueryResult<T> list(Class<T> c, Query query) {
		try {
			// TODO(eddiemay) Need to set values from query.
			StringBuilder url = new StringBuilder(apiConnector.getApiUrl() + "/" + getResourceName(c) + "?");

			url.append(query.getFilterList().stream()
					.map(filter -> filter.getColumn() + "=" + filter.getOperator() + filter.getValue())
					.collect(Collectors.joining("&")));
			if (query.getOrderByCount() > 0) {
				url.append("&orderBy").append("=").append(query.getOrderByList().stream()
						.map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
						.collect(Collectors.joining(",")));
			}
			if (query.getLimit() > 0) {
				url.append("&pageSize").append("=").append(query.getLimit());
			}
			if (query.getOffset() > 0) {
				url.append("&pageToken").append("=").append(query.getOffset());
			}
			String response = apiConnector.sendGet(url.toString());
			ListResponse.Builder builder = ListResponse.newBuilder();
			T type = DAOCloudDS.getDefaultInstance(c);
			JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(type.getDescriptorForType()).build();
			JsonFormat.parser().usingTypeRegistry(registry).merge(response, builder);
			ListResponse listResponse = builder.build();
			return new QueryResult<>(
					listResponse.getResultList().stream()
							.map(any -> any.unpack(c))
							.collect(Collectors.toList()),
					listResponse.getTotalSize());
		} catch (IOException ioe) {
			throw new DD4StorageException(ioe);
		}
	}

	@Override
	public <T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return new RetryableFunction<Pair<Long, UnaryOperator<T>>, T>() {
			@Override
			public T apply(Pair<Long, UnaryOperator<T>> pair) {
				long id = pair.getLeft();
				UnaryOperator<T> updater = pair.getRight();
				T orig = get(c, id);
				T updated = updater.apply(orig);

				// Find all the fields that were modified in the updated proto.
				Map<FieldDescriptor, Object> valueMap = updated.getAllFields();
				List<FieldDescriptor> modified = new ArrayList<>();
				for (Map.Entry<FieldDescriptor, Object> entry : valueMap.entrySet()) {
					FieldDescriptor field = entry.getKey();
					if (!valueMap.get(field).equals(orig.getField(field))) {
						modified.add(field);
					}
				}

				// Find all the fields that have been removed from the update set them to null.
				for (FieldDescriptor field : orig.getAllFields().keySet()) {
					if (!valueMap.containsKey(field)) {
						modified.add(field);
					}
				}
				if (modified.isEmpty()) {
					System.out.println("Nothing changed, returning");
				} else {
					try {
						String url = apiConnector.getApiUrl() + "/" + getResourceName(c) + "/" + id;
						UpdateRequest request = UpdateRequest.newBuilder()
								.setId(id)
								.setEntity(Any.pack(updated))
								.setUpdateMask(FieldMask.newBuilder()
										.addAllPaths(modified.stream().map(FieldDescriptor::getName).collect(Collectors.toList())))
								.build();
						JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(updated.getDescriptorForType()).build();
						Printer jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
						apiConnector.send("POST", url, String.format(API_PAYLOAD, jsonPrinter.print(request)));
					} catch (IOException ioe) {
						throw new DD4StorageException("Error updating record " + updated + ": " + ioe.getMessage(), ioe);
					}
				}
				return get(c, id);
			}
		}.applyWithRetries(Pair.of(id, updater));
	}

	@Override
	public <T> void delete(Class<T> c, long id) {
		String url = apiConnector.getApiUrl() + "/" + getResourceName(c) + "/" + id;
		try {
			apiConnector.send("DELETE", url, null);
		} catch (IOException ioe) {
			throw new DD4StorageException(ioe);
		}
	}

	private static String getResourceName(Class<?> cls) {
		return FormatText.toLowerCamel(cls.getSimpleName()) + "s";
	}
}
