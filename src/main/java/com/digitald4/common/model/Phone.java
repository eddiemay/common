package com.digitald4.common.model;

public class Phone {
  private String number;
  private Long typeId;

  public String getNumber() {
    return number;
  }

  public Phone setNumber(String number) {
    this.number = number;
    return this;
  }

  public Long getTypeId() {
    return typeId;
  }

  public Phone setTypeId(Long typeId) {
    this.typeId = typeId;
    return this;
  }

  @Override
  public String toString() {
    return String.format("%s%s", typeId == null ? "" : typeId + " ", number);
  }
}
