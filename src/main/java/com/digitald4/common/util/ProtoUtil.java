package com.digitald4.common.util;

import com.digitald4.common.exception.DD4StorageException;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class ProtoUtil {
	private static final Map<Class<?>, Message> defaultInstances = new HashMap<>();
	private static Parser parser = JsonFormat.parser();
	private static Printer printer = JsonFormat.printer();

	private ProtoUtil() {}

	public static void init(Descriptor... descriptors) {
		TypeRegistry registry = TypeRegistry.newBuilder().add(Arrays.stream(descriptors).collect(Collectors.toList())).build();
		parser = parser.usingTypeRegistry(registry);
		printer = printer.usingTypeRegistry(registry);
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

	public static String print(Message message) {
		try {
			return printer.print(message);
		} catch (InvalidProtocolBufferException e) {
			throw new DD4StorageException("Error converting message to json: " + message, e);
		}
	}

	public <R extends Message> R toProto(R msgRequest, HttpServletRequest request) {
		return toProto(msgRequest, new JSONObject(request.getParameterMap().values().iterator().next()[0]));
	}

	@SuppressWarnings("unchecked")
	public static <R extends Message> R toProto(R msgRequest, JSONObject json) {
		R.Builder builder = msgRequest.toBuilder();
		ProtoUtil.merge(json, builder);
		return (R) builder.build();
	}

	public static JSONObject toJSON(Message item) {
		return new JSONObject(ProtoUtil.print(item));
	}

	public static JSONObject toJSON(boolean bool) {
		return new JSONObject(bool);
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
}
