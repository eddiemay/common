package com.digitald4.common.storage;

import com.digitald4.common.model.ModelObject;

import javax.inject.Inject;
import javax.inject.Provider;

public class ModelObjectStore<ID, T extends ModelObject<ID>> extends GenericStore<T, ID> {

  @Inject
  public ModelObjectStore(T type, Provider<DAO> daoProvider) {
    super(type, daoProvider);
  }

  public ModelObjectStore(Class<T> c, Provider<DAO> daoProvider) {
    super(c, daoProvider);
  }
}
