package com.digitald4.common.server.service;

import com.digitald4.common.storage.Store;

public interface EntityService<T> {
  Store<T> getStore();
}
