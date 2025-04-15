package com.digitald4.common.storage;

import javax.inject.Inject;
import javax.inject.Provider;

public class GenericStringStore<T> extends GenericStore<T, String> implements Store<T, String> {

  @Inject
  public GenericStringStore(T type, Provider<DAO> daoProvider) {
    super(type, daoProvider);
  }

  public GenericStringStore(Class<T> c, Provider<DAO> daoProvider) {
    super(c, daoProvider);
  }
}
