package com.digitald4.common.model;

public class FileReference {
  private long id;
  private String name;
  private String type;
  private int size;

  public static FileReference of(DataFile dataFile) {
    return new FileReference()
        .setId(dataFile.getId())
        .setName(dataFile.getName())
        .setType(dataFile.getType())
        .setSize(dataFile.getSize());
  }

  public long getId() {
    return id;
  }

  public FileReference setId(long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public FileReference setName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  public FileReference setType(String type) {
    this.type = type;
    return this;
  }

  public int getSize() {
    return size;
  }

  public FileReference setSize(int size) {
    this.size = size;
    return this;
  }
}
