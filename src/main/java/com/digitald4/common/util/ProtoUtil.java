package com.digitald4.common.util;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.OrderBy;
import com.digitald4.common.proto.DD4UIProtos.BatchDeleteRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.QueryResult;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.FieldMaskUtil.MergeOptions;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class ProtoUtil {
	private static final MergeOptions MERGE_OPTIONS = new MergeOptions();
	static {
		MERGE_OPTIONS.setReplaceRepeatedFields(true);
	}
	private static final Map<Class<?>, Message> defaultInstances = new HashMap<>();
	private static Parser parser = JsonFormat.parser();
	private static Printer printer = JsonFormat.printer();

	private ProtoUtil() {}

	public static <T extends Message> T merge(JSONObject json, T type) {
		T.Builder builder = type.toBuilder();
		merge(json.toString(), builder);
		return (T) builder.build();
	}

	public static void merge(JSONObject json, Message.Builder builder) {
		merge(json.toString(), builder);
	}

	public static void merge(String json, Message.Builder builder) {
		try {
			parser.merge(json, builder);
		} catch (InvalidProtocolBufferException e) {
			throw new DD4StorageException("Error merging json", e);
		}
	}

	public static <T extends Message> T merge(FieldMask fieldMask, T fromProto, T toProto) {
		T.Builder builder = toProto.toBuilder();
		merge(fieldMask, fromProto, builder);
		return (T) builder.build();
	}

	public static <T extends Message> HasProto<T> merge(FieldMask fieldMask, HasProto<T> fromEntity, HasProto<T> toEntity)
	{
		return toEntity.setProto(merge(fieldMask, fromEntity.getProto(), toEntity.getProto()));
	}

	public static <T extends Message> void merge(FieldMask fieldMask, T fromProto, T.Builder toBuilder) {
		FieldMaskUtil.merge(fieldMask, fromProto, toBuilder, MERGE_OPTIONS);
	}

	public static String print(Message message) {
		try {
			return printer.print(message);
		} catch (InvalidProtocolBufferException e) {
			throw new DD4StorageException("Error converting message to json: " + message, e);
		}
	}

	public static <T extends Message> T toProto(T msgRequest, HttpServletRequest request) {
		return toProto(msgRequest, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Message> T toProto(T msgRequest, JSONObject json) {
		T.Builder builder = msgRequest.toBuilder();
		ProtoUtil.merge(json, builder);
		return (T) builder.build();
	}

	public static JSONObject toJSON(Message item) {
		return new JSONObject(ProtoUtil.print(item));
	}

	public static JSONObject toJSON(HasProto item) {
		return toJSON(item.toProto());
	}

	public static JSONObject toJSON(boolean bool) {
		return new JSONObject(bool);
	}

	public static <T extends Message> JSONObject toJSON(QueryResult<T> queryResult) {
		return new JSONObject().put("totalSize", queryResult.getTotalSize())
				.put("result", queryResult.getResults().stream().map(ProtoUtil::toJSON).collect(Collectors.toList()));
	}

	public static <T extends Message> T unpack(Class<T> c, Any any) {
		try {
			return any.unpack(c);
		} catch (InvalidProtocolBufferException e) {
			throw new DD4StorageException(String.format("Error unpacking any: %s of type %s ", any, c), e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Message> T getDefaultInstance(Class<T> c) {
		return (T) defaultInstances.computeIfAbsent(c, cls -> {
			try {
				return (T) c.getMethod("getDefaultInstance").invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new DD4StorageException("Error getting default instance for type: " + c, e);
			}
		});
	}

	public static Query.Filter createFilter(String column, String operator, Object value) {
		return Query.Filter.newBuilder().setColumn(column).setOperator(operator).setValue(String.valueOf(value)).build();
	}

	public static Query toQuery(ListRequest request) {
		Query.Builder query = Query.newBuilder()
				.setLimit(request.getPageSize())
				.setOffset(request.getPageToken());
		if (!request.getFilter().isEmpty()) {
			query.addAllFilter(Arrays.stream(request.getFilter().split(","))
					.map(filter -> {
						String[] parts = filter.split(" ");
						return Query.Filter.newBuilder()
								.setColumn(parts[0])
								.setOperator(parts[1])
								.setValue(parts[2])
								.build();
					})
					.collect(Collectors.toList()));
		}
		if (!request.getOrderBy().isEmpty()) {
			query.addAllOrderBy(Arrays.stream(request.getOrderBy().split(","))
					.map(orderBy -> OrderBy.newBuilder()
							.setColumn(orderBy.split(" ")[0])
							.setDesc(orderBy.endsWith("DESC"))
							.build())
					.collect(Collectors.toList()));
		}
		return query.build();
	}

	public static Query toQuery(BatchDeleteRequest request) {
		Query.Builder query = Query.newBuilder()
				.setLimit(request.getPageSize())
				.setOffset(request.getPageToken())
				.addAllFilter(request.getFilterList().stream()
						.map(filter -> Query.Filter.newBuilder()
								.setColumn(filter.getColumn())
								.setOperator(filter.getOperator())
								.setValue(filter.getValue())
								.build())
						.collect(Collectors.toList()));
		if (!request.getOrderBy().isEmpty()) {
			query.addAllOrderBy(Arrays.stream(request.getOrderBy().split(","))
					.map(orderBy -> OrderBy.newBuilder()
							.setColumn(orderBy.split(" ")[0])
							.setDesc(orderBy.endsWith("DESC"))
							.build())
					.collect(Collectors.toList()));
		}
		return query.build();
	}
}
