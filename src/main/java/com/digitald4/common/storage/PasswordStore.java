package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.PasswordInfo;

import javax.inject.Inject;
import javax.inject.Provider;

public class PasswordStore  extends GenericStore<PasswordInfo> {
  @Inject
  public PasswordStore(Provider<DAO> daoProvider) {
    super(PasswordInfo.class, daoProvider);
  }

  /**
   *  Validates a password making sure it is propertly encoded
   *
   * @param password to validate
   * @throws DD4StorageException if the password is not encoded correctly.
   * @return The password if it is valid
   */
  public static String validate(String password) {
    password.chars().forEach(c -> {
      if (c > 'F') {
        throw new DD4StorageException(
            "None encrypted password detected. Passwords must be encrypted", DD4StorageException.ErrorCode.BAD_REQUEST);
      }
    });

    return password;
  }
}
