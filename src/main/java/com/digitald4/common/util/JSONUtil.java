package com.digitald4.common.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.exception.DD4StorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtil {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private JSONUtil() {}

  public static <T> ImmutableList<T> transform(JSONArray jsonArray, Function<Integer, T> converter) {
    return IntStream.range(0, jsonArray.length())
        .mapToObj(converter::apply)
        .collect(toImmutableList());
  }

  public static <T> T merge(String updateMask, T fromEntity, T toEntity) {
    JSONObject fromJson = new JSONObject(fromEntity);
    JSONObject toJSON = new JSONObject(toEntity);
    Arrays.stream(updateMask.split(",")).forEach(path -> toJSON.put(path, fromJson.get(path)));

    return convertTo((Class<T>) toEntity.getClass(), toJSON);
  }

  public static <T> T convertTo(Class<T> cls, JSONObject jsonObject) {
    return convertTo(cls, jsonObject.toString());
  }

  public static <T> T convertTo(Class<T> cls, String json) {
    try {
      return MAPPER.readValue(json.toString(), cls);
    } catch (IOException e) {
      throw new DD4StorageException("Error reading json object: " + json + " of type: " + cls, e);
    }
  }
}
