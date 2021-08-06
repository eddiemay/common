package com.digitald4.common.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.digitald4.common.exception.DD4StorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtil {
  private static final Map<Class<?>, Object> defaultInstances = new HashMap<>();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private JSONUtil() {}

  public static <T> ImmutableList<T> transform(JSONArray jsonArray, Function<Integer, T> converter) {
    return IntStream.range(0, jsonArray.length())
        .mapToObj(converter::apply)
        .collect(toImmutableList());
  }

  public static <T> JSONObject toJSON(T object) {
    if (object instanceof Message) {
      return ProtoUtil.toJSON(object);
    }

    return new JSONObject(object);
  }

  public static <T> T merge(String updateMask, T fromEntity, T toEntity) {
    if (fromEntity instanceof Message) {
      return (T) ProtoUtil.merge(updateMask, (Message) fromEntity, (Message) toEntity);
    }

    JSONObject fromJson = new JSONObject(fromEntity);
    JSONObject toJSON = new JSONObject(toEntity);
    stream(updateMask.split(",")).forEach(path -> toJSON.put(path, fromJson.get(path)));

    return toObject((Class<T>) toEntity.getClass(), toJSON);
  }

  public static <T> T toObject(Class<T> cls, JSONObject jsonObject) {
    if (jsonObject == null) {
      return null;
    }

    if (isProto(cls)) {
      return (T) ProtoUtil.toProto(ProtoUtil.getDefaultInstance((Class<Message>) cls), jsonObject);
    }

    return toObject(cls, jsonObject.toString());
  }

  public static <T> T toObject(Class<T> cls, String json) {
    try {
      return MAPPER.readValue(json, cls);
    } catch (IOException e) {
      throw new DD4StorageException("Error reading json object: " + json + " of type: " + cls, e);
    }
  }

  public static boolean isProto(Class<?> cls) {
    return cls.getSuperclass() == GeneratedMessageV3.class;
  }

  public static <T> T getDefaultInstance(Class<T> cls) {
    return (T) defaultInstances.computeIfAbsent(cls, c -> newInstance(c));
  }

  public static <T> T newInstance(Class<T> cls) {
    if (isProto(cls)) {
      return (T) ProtoUtil.getDefaultInstance((Class<Message>) cls);
    }

    try {
      return cls.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new DD4StorageException("Error getting default instance for type: " + cls, e);
    }
  }
}
