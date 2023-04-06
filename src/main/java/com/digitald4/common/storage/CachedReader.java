package com.digitald4.common.storage;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class CachedReader {
  private final Map<String, Object> cachedItems = new HashMap<>();
  private final DAO dao;

  public CachedReader(DAO dao) {
    this.dao = dao;
  }

  public <T, I> T get(Class<T> c, @Nullable I id) {
    if (id == null) {
      return null;
    }

    return (T) cachedItems.computeIfAbsent(getIdString(c, id), idStr -> dao.get(c, id));
  }

  private <T> String getIdString(Class<T> c, Object id) {
    return String.format("%s-%s", c.getSimpleName(), id);
  }
}
