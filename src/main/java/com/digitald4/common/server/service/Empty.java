package com.digitald4.common.server.service;

public class Empty {
  private static final Empty instance = new Empty();

  public static Empty getInstance() {return instance;}

  // GCP requires at least 1 property to be serializable.
  public String getMessage() {return null;}
}
