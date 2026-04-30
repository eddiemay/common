package com.digitald4.common.model;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

public class DataFile extends ModelObjectModUser<String> {
  private String id;
  private String name;
  private String type;
  private int size;
  private String entityType;
  private String entityId;
  private String comment;
  private byte[] data;

  public String getId() {
    return id;
  }

  public DataFile setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public DataFile setName(String name) {
    this.name = name;
    if (id == null) {
      this.id = name;
    }
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

  public String getEntityType() {
    return entityType;
  }

  public DataFile setEntityType(String entityType) {
    this.entityType = entityType;
    return this;
  }

  public String getEntityId() {
    return entityId;
  }

  public DataFile setEntityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public DataFile setComment(String comment) {
    this.comment = comment;
    return this;
  }

  @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
  public byte[] getData() {
    return data;
  }

  public DataFile setData(byte[] data) {
    this.data = data;
    this.size = data.length;
    return this;
  }
}
