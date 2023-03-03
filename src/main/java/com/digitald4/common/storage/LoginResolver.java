package com.digitald4.common.storage;

import com.digitald4.common.model.Session;

public interface LoginResolver {
  Session resolve(String token, boolean loginRequired);
}
