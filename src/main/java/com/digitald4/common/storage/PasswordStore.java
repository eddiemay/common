package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.Password;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;

public class PasswordStore extends GenericStore<Password> {
  public static final DD4StorageException BAD_LOGIN =
    new DD4StorageException("Wrong username or password", DD4StorageException.ErrorCode.NOT_AUTHENTICATED);
  private final Clock clock;

  @Inject
  public PasswordStore(Provider<DAO> daoProvider, Clock clock) {
    super(Password.class, daoProvider);
    this.clock = clock;
  }

  public boolean verify(long userId, String passwordHash) {
    validateEncoding(passwordHash);

    Password password =
        list(Query.forValues("userId=" + userId, null, 0, 0))
            .getResults().stream().findFirst().orElse(null);
    if (password == null || !password.getDigest().equals(passwordHash)) {
      if (password == null) {
        throw new DD4StorageException("Password record not found for userId: " + userId, DD4StorageException.ErrorCode.NOT_AUTHENTICATED);
      }
      throw new DD4StorageException("Password does not match", DD4StorageException.ErrorCode.NOT_AUTHENTICATED);
    }

    return true;
  }

  public boolean updatePassword(long userId, String passwordHash) {
    validateEncoding(passwordHash);
    DateTime now = new DateTime(clock);
    create(new Password().setUserId(userId).setDigest(passwordHash).setCreatedAt(now));

    return true;
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
        throw new DD4StorageException(
            "None encrypted password detected. Passwords must be encrypted", DD4StorageException.ErrorCode.BAD_REQUEST);
      }
    });
  }
}
