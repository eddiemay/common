package com.digitald4.common.model;

import com.digitald4.common.util.JSONUtil;
import java.util.Objects;

public class ModelObject<ID> {

  private ID id;

  public ID getId() {
    return id;
  }

  public ModelObject<ID> setId(ID id) {
    this.id = id;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    return Objects.equals(toString(), obj.toString());
  }

  @Override
  public String toString() {
    return JSONUtil.toJSON(this).toString();
  }
}
