package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.Password;
import com.digitald4.common.storage.Query.OrderBy;
import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Provider;

public class PasswordStore extends GenericStore<Password, Long> {
  public static final DD4StorageException BAD_LOGIN =
      new DD4StorageException("Wrong username or password", ErrorCode.NOT_AUTHENTICATED);

  @Inject
  public PasswordStore(Provider<DAO> daoProvider) {
    super(Password.class, daoProvider);
  }

  public boolean verify(long userId, String passwordHash) {
    validateEncoding(passwordHash);

    var passwords = list(Query.forList(Query.Filter.of("userId", userId)).setOrderBys(OrderBy.of("createdAt", true))).getItems();
    if (passwords.isEmpty()) {
      throw new DD4StorageException("Password record not found for userId: " + userId, ErrorCode.NOT_AUTHENTICATED);
    } else if (!passwords.get(0).getDigest().equals(passwordHash)) {
      throw new DD4StorageException("Password does not match", ErrorCode.NOT_AUTHENTICATED);
    }

    return true;
  }

  public void updatePassword(long userId, String passwordHash) {
    validateEncoding(passwordHash);
    create(new Password().setUserId(userId).setDigest(passwordHash).setCreatedAt(Instant.now()));
  }

  /**
   *  Validates a password making sure it is propertly encoded
   *
   * @param password to validate
   * @throws DD4StorageException if the password is not encoded correctly.
   */
  public static void validateEncoding(String password) {
    password.chars().forEach(c -> {
      if (c > 'F') {
        throw new DD4StorageException("None encrypted password detected. Must be encrypted", ErrorCode.BAD_REQUEST);
      }
    });
  }
}
