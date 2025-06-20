package com.digitald4.common.storage;

public interface DAO extends TypedDAO<Object> {
  public enum Context {PROD, TEST, NONE}
}
