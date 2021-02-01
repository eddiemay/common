package com.digitald4.common.storage;

import com.digitald4.common.model.HasProto;
import javax.inject.Provider;

public class ModelStore<T extends HasProto> extends GenericStore<HasProto, T> {

  public ModelStore(Class<T> c, Provider<DAO<HasProto>> daoProvider) {
    super(c, daoProvider);
  }
}
