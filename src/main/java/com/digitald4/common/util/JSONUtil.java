package com.digitald4.common.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.Annotations;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtil {

  private static final Map<Class<?>, Object> defaultInstances = new HashMap<>();
  private static final Map<Class<?>, ImmutableMap<String, Field>> typeFields = new HashMap<>();
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private JSONUtil() {
  }

  public static <T> ImmutableList<T> transform(JSONArray jsonArray,
      Function<Integer, T> converter) {
    return IntStream.range(0, jsonArray.length())
        .mapToObj(converter::apply)
        .collect(toImmutableList());
  }

  public static <T> JSONObject toJSON(T object) {
    return new JSONObject(object);
  }

  public static <T> T merge(String updateMask, T fromEntity, T toEntity) {
    JSONObject fromJson = new JSONObject(fromEntity);
    JSONObject toJSON = new JSONObject(toEntity);
    stream(updateMask.split(",")).forEach(path -> toJSON.put(path, fromJson.get(path)));

    return toObject((Class<T>) toEntity.getClass(), toJSON);
  }

  public static <T> T toObject(Class<T> cls, JSONObject jsonObject) {
    if (jsonObject == null) {
      return null;
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

  public static <T> T copy(T t) {
    return toObject((Class<T>) t.getClass(), toJSON(t));
  }

  public static <T> T getDefaultInstance(Class<T> cls) {
    return (T) defaultInstances.computeIfAbsent(cls, c -> newInstance(c));
  }

  public static <T> T newInstance(Class<T> cls) {
    try {
      return cls.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new DD4StorageException("Error getting default instance for type: " + cls, e);
    }
  }

  public static ImmutableMap<String, Field> getFields(Class<?> c) {
    return typeFields.computeIfAbsent(c, v -> {
      Map<String, Method> methods = new HashMap<>();
      stream(c.getMethods()).forEach(method -> methods.put(method.getName(), method));

      return methods.values().stream()
          .filter(m -> m.getParameters().length == 0
              && (m.getName().startsWith("get") || m.getName().startsWith("is")))
          .map(method -> {
            String name = method.getName().substring(method.getName().startsWith("is") ? 2 : 3);
            return new Field(name, method, methods.get("set" + name));
          })
          .collect(toImmutableMap(Field::getName, identity()));
    });
  }

  public static class Field {
    private final String name;
    private final Class<?> type;
    private final Method getMethod;
    private final Method setMethod;
    private final boolean nonIndexed;

    public Field(String name, Method getMethod, Method setMethod) {
      this.name = name.substring(0, 1).toLowerCase() + name.substring(1);
      this.type = getMethod.getReturnType();
      this.getMethod = getMethod;
      this.setMethod = setMethod;
      nonIndexed = getMethod.getAnnotation(Annotations.NonIndexed.class) != null;
    }

    public String getName() {
      return name;
    }

    public Class<?> getType() {
      return type;
    }

    public Method getGetMethod() {
      return getMethod;
    }

    public Method getSetMethod() {
      return setMethod;
    }

    public boolean isNonIndexed() {
      return nonIndexed;
    }

    public <T> T invokeSet(T t, Object value) {
      try {
        setMethod.invoke(t, value);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new DD4StorageException("Error invoking set method", e);
      }
      return t;
    }

    public boolean isObject() {
      return !getType().isPrimitive() && getType() != String.class && !getType().isEnum()
          && getType() != Object.class;
    }

    public boolean isCollection() {
      return isCollection(getType());
    }

    public static boolean isCollection(Class<?> cls) {
      return cls.getName().startsWith("com.google.common.collect.") || cls.getName().startsWith("java.util.");
    }

    @Override
    public String toString() {
      return String.format("%s %s", getType(), getName());
    }
  }
}
