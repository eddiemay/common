package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Descriptors.FieldDescriptor;
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
import javax.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

public class DAOApiProtoImpl implements TypedDAO<Message> {
	private final APIConnector apiConnector;
	private final Parser jsonParser;

	@Inject
	public DAOApiProtoImpl(APIConnector apiConnector) {
		this.apiConnector = apiConnector;
		jsonParser = JsonFormat.parser();
	}

	@Override
	public <T extends Message> T create(T t) {
		String url = apiConnector.formatUrl(getResourceName(t.getClass())) + "/_";
		JsonFormat.TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(t.getDescriptorForType()).build();
		Printer jsonPrinter = JsonFormat.printer().usingTypeRegistry(registry);
		Parser jsonParser = JsonFormat.parser().usingTypeRegistry(registry);
		try {
			Message.Builder builder = t.newBuilderForType();
			jsonParser.merge(apiConnector.sendPost(url, jsonPrinter.print(t)), builder);
			return (T) builder.build();
		} catch (Exception ioe) {
			throw new DD4StorageException(ioe);
		}
	}

	@Override
	public <T extends Message> T get(Class<T> c, long id) {
		String url = apiConnector.formatUrl(getResourceName(c)) + "/" + id;
		String json = apiConnector.sendGet(url);
		Message.Builder builder = ProtoUtil.getDefaultInstance(c).toBuilder();
		ProtoUtil.merge(json, builder);
		return (T) builder.build();
	}

	@Override
	public <T extends Message> QueryResult<T> list(Class<T> c, Query query) {
		StringBuilder url = new StringBuilder(apiConnector.formatUrl(getResourceName(c)) + "/_?");

		url.append("filter=").append(query.getFilters().stream()
				.map(filter -> filter.getColumn() + filter.getOperator() + filter.getValue())
				.collect(Collectors.joining(",")));
		if (!query.getOrderBys().isEmpty()) {
			url.append("&orderBy=").append(query.getOrderBys().stream()
					.map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
					.collect(Collectors.joining(",")));
		}
		if (query.getLimit() > 0) {
			url.append("&pageSize").append("=").append(query.getLimit());
		}
		if (query.getOffset() > 0) {
			url.append("&pageToken").append("=").append(query.getOffset());
		}
		JSONObject response = new JSONObject(apiConnector.sendGet(url.toString()));

		T type = ProtoUtil.getDefaultInstance(c);
		int totalSize = response.getInt("totalSize");
		List<T> results = new ArrayList<>(totalSize);
		if (totalSize > 0) {
			JSONArray resultArray = response.getJSONArray("results");
			for (int x = 0; x < resultArray.length(); x++) {
				results.add(ProtoUtil.merge(resultArray.getJSONObject(x), type));
			}
		}
		return new QueryResult<>(results, response.getInt("totalSize"));
	}

	@Override
	public <T extends Message> T update(Class<T> c, long id, UnaryOperator<T> updater) {
		return Calculate.executeWithRetries(2, () -> {
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
					String updateMask = modified.stream().map(FieldDescriptor::getName).collect(Collectors.joining());
					String url = apiConnector.formatUrl(getResourceName(c)) + "/" + id + "?updateMask=" + updateMask;
					String json = apiConnector.send("POST", url, new JSONObject(updated).toString());
					Message.Builder builder = ProtoUtil.getDefaultInstance(c).toBuilder();
					jsonParser.merge(json, builder);
					return (T) builder.build();
				} catch (IOException ioe) {
					throw new DD4StorageException("Error updating record " + updated + ": " + ioe.getMessage(), ioe);
				}
			}
			return updated;
		});
	}

	@Override
	public <T extends Message> void delete(Class<T> c, long id) {
		String url = apiConnector.formatUrl(getResourceName(c)) + "/" + id;
		apiConnector.send("DELETE", url, null);
	}

	@Override
	public <T extends Message> int delete(Class<T> c, Query query) {
		StringBuilder url = new StringBuilder(apiConnector.formatUrl(getResourceName(c)) + ":batchDelete?");

		url.append(query.getFilters().stream()
				.map(filter -> filter.getColumn() + "=" + filter.getOperator() + filter.getValue())
				.collect(Collectors.joining("&")));
		if (!query.getOrderBys().isEmpty()) {
			url.append("&orderBy").append("=").append(query.getOrderBys().stream()
					.map(orderBy -> orderBy.getColumn() + (orderBy.getDesc() ? " DESC" : ""))
					.collect(Collectors.joining(",")));
		}
		if (query.getLimit() > 0) {
			url.append("&pageSize").append("=").append(query.getLimit());
		}
		if (query.getOffset() > 0) {
			url.append("&pageToken").append("=").append(query.getOffset());
		}
		JSONObject response = new JSONObject(apiConnector.send("DELETE", url.toString(), null));
		return response.getInt("deleted");
	}

	private static String getResourceName(Class<?> cls) {
		return FormatText.toLowerCamel(cls.getSimpleName()) + "s";
	}
}
