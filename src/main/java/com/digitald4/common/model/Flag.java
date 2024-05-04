package com.digitald4.common.model;

public class Flag {
  private String id;
  private Object value;

  public String getId() {
    return id;
  }

  public Flag setId(String id) {
    this.id = id;
    return this;
  }

  public Object getValue() {
    return value;
  }

  public Flag setValue(Object value) {
    this.value = value;
    return this;
  }
}
