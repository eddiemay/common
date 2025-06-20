package com.digitald4.common.model;

public class Identifier<I> {
  private I id;
  private String name;

  public I getId() {
    return id;
  }

  public Identifier<I> setId(I id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Identifier<I> setName(String name) {
    this.name = name;
    return this;
  }

  public static <I> Identifier<I> of(ModelObject<I> modelObject) {
    return of(modelObject.getId(), modelObject.toString());
  }

  public static <I> Identifier<I> of(I id, String name) {
    return new Identifier<I>().setId(id).setName(name);
  }
}
