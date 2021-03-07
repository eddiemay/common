package com.digitald4.common.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import org.json.JSONArray;

import java.util.function.Function;
import java.util.stream.IntStream;

public class JSONUtil {
  private JSONUtil() {}

  public static <T> ImmutableList<T> transform(JSONArray jsonArray, Function<Integer, T> converter) {
    return IntStream.range(0, jsonArray.length())
        .mapToObj(converter::apply)
        .collect(toImmutableList());
  }
}
