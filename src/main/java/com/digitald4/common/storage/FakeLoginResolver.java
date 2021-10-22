package com.digitald4.common.storage;

import com.digitald4.common.model.Session;

public class FakeLoginResolver implements LoginResolver {

  public Session resolve(String token, boolean loginRequired) {
    return null;
  }
}
