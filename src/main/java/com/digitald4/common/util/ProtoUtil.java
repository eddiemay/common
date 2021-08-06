package com.digitald4.common.util;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.HasProto;
import com.digitald4.common.storage.QueryResult;
import com.google.common.collect.ImmutableList;
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
	private static final Parser parser = JsonFormat.parser();
	private static final Printer printer = JsonFormat.printer();

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

	public static <T extends Message> T merge(FieldMask fieldMask, T fromProto, T.Builder builder) {
		FieldMaskUtil.merge(fieldMask, fromProto, builder, MERGE_OPTIONS);
		return (T) builder.build();
	}

	public static <T extends Message> T merge(FieldMask fieldMask, T fromProto, T toProto) {
		return merge(fieldMask, fromProto, toProto.toBuilder());
	}

	public static <T extends Message> T merge(String updateMask, T fromProto, T toProto) {
		return merge(toFieldMask(updateMask), fromProto, toProto.toBuilder());
	}

	public static <P extends Message, T extends HasProto<P>> T merge(FieldMask fieldMask, T fromEntity, T toEntity) {
		toEntity.fromProto(merge(fieldMask, fromEntity.toProto(), toEntity.toProto()));
		return toEntity;
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

	public static JSONObject toJSON(boolean bool) {
		return new JSONObject(bool);
	}

	public static JSONObject toJSON(Object o) {
		if (o instanceof Message) {
			return new JSONObject(ProtoUtil.print((Message) o));
		}

		if (o instanceof HasProto) {
			return toJSON(((HasProto<?>) o).toProto());
		}
		return new JSONObject(o);
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

	public static FieldMask toFieldMask(String updateMask) {
		return FieldMask.newBuilder()
				.addAllPaths(isNullOrEmpty(updateMask) ? ImmutableList.of() : Arrays.asList(updateMask.split(",")))
				.build();
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
}
