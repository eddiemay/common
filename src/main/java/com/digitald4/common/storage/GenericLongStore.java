package com.digitald4.common.storage;

import javax.inject.Inject;
import javax.inject.Provider;

public class GenericLongStore<T> extends GenericStore<T, Long> implements LongStore<T> {

  @Inject
  public GenericLongStore(T type, Provider<DAO> daoProvider) {
    super(type, daoProvider);
  }

  public GenericLongStore(Class<T> c, Provider<DAO> daoProvider) {
    super(c, daoProvider);
  }
}
