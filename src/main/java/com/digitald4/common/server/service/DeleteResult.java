package com.digitald4.common.server.service;

public class DeleteResult {
  private final int count;

  private DeleteResult(int count) {
    this.count = count;
  }

  public int getCount() {
    return count;
  }

  public static DeleteResult of(int count) {
    return new DeleteResult(count);
  }
}
