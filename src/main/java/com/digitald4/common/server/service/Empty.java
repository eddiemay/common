package com.digitald4.common.server.service;

public class Empty {
  private static final Empty instance = new Empty();

  private Empty() {}

  public static Empty getInstance() {
    return instance;
  }
}
