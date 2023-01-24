package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

public class DataFile {
  private long id;
  private String name;
  private String type;
  private int size;
  private byte[] data;

  public long getId() {
    return id;
  }

  public DataFile setId(long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public DataFile setName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  public DataFile setType(String type) {
    this.type = type;
    return this;
  }

  public int getSize() {
    return size;
  }

  public DataFile setSize(int size) {
    this.size = size;
    return this;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public byte[] getData() {
    return data;
  }

  public DataFile setData(byte[] data) {
    this.data = data;
    return this;
  }
}
