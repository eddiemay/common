package com.digitald4.common.server.service;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.storage.PasswordStore;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.UserStore;

import javax.inject.Inject;

public class BasicUserService extends UserService<BasicUser> {
  @Inject
  public BasicUserService(
      UserStore<BasicUser> userStore, SessionStore<BasicUser> sessionStore, PasswordStore passwordStore) {
    super(userStore, sessionStore, passwordStore);
  }
}
