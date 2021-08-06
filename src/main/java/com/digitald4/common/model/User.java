package com.digitald4.common.model;

import java.time.Clock;

public interface User {

	long getId();

	String getUsername();

	User setUsername(String username);

	int getTypeId();

	long getLastLogin();

	User updateLastLogin(Clock clock);

	User updatePasswordInfo(PasswordInfo passwordInfo);

	ActiveSession activeSession();

	User activeSession(ActiveSession activeSession);

	/**
	 * Attemps to verfiy the password provided by the user.
	 * @throws com.digitald4.common.exception.DD4StorageException if password does not match
	 * @param passwordDigest
	 */
	void verifyPassword(String passwordDigest);
}
