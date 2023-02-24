package com.digitald4.common.server.service;

import com.google.common.collect.ImmutableList;

public class IterableParam<T> {
  private ImmutableList<T> items;

  public ImmutableList<T> getItems() {
    return items;
  }

  public IterableParam<T> setItems(Iterable<T> items) {
    this.items = ImmutableList.copyOf(items);
    return this;
  }
}
