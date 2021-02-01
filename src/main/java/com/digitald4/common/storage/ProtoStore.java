package com.digitald4.common.storage;

import com.digitald4.common.util.ProtoUtil;
import com.google.protobuf.Message;
import javax.inject.Provider;

public class ProtoStore<T extends Message> extends GenericStore<Message, T> {

  private final T type;

  public ProtoStore(Class<T> c, Provider<DAO<Message>> daoProvider) {
    super(c, daoProvider);
    this.type = ProtoUtil.getDefaultInstance(c);
  }

  public T getType() {
    return type;
  }
}
