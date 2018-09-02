package com.digitald4.common.util;

import com.digitald4.common.exception.DD4StorageException;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class ProtoUtil {
	private static final Map<Class<?>, Message> defaultInstances = new HashMap<>();
	private static final Parser ignoringUnknownFieldsParser = JsonFormat.parser().ignoringUnknownFields();
	private static final Map<Class<?>, Parser> parsers = new HashMap<>();
	private static final Printer printer = JsonFormat.printer();

	private ProtoUtil() {}

	public static void merge(JSONObject json, Message.Builder builder) {
		merge(
				parsers.computeIfAbsent(
						builder.getClass(),
						c -> JsonFormat.parser().usingTypeRegistry(TypeRegistry.newBuilder().add(builder.build().getDescriptorForType()).build())),
				json.toString(),
				builder);
	}

	public static void merge(String json, Message.Builder builder) {
		merge(ignoringUnknownFieldsParser, json, builder);
	}

	private static void merge(Parser parser, String json, Message.Builder builder) {
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
