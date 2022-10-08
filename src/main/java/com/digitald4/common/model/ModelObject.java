package com.digitald4.common.model;

public interface ModelObject<ID> {

  ID getId();

  ModelObject<ID> setId(ID id);
}
