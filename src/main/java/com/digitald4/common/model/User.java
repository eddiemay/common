package com.digitald4.common.model;

import com.digitald4.common.proto.DD4Protos.ActiveSession;
import com.google.protobuf.Message;
import java.time.Clock;

public interface User<P extends Message, PI> extends HasProto<P> {

	long getId();

	int getTypeId();

	String getUsername();

	User setUsername(String username);

	long getLastLogin();

	User updateLastLogin(Clock clock);

	User setPasswordInfo(PI passwordInfo);

	ActiveSession getActiveSession();

	User setActiveSession(ActiveSession activeSession);

	/**
	 * Attemps to verfiy the password provided by the user.
	 * @throws com.digitald4.common.exception.DD4StorageException if password does not match
	 * @param passwordDigest
	 */
	void verifyPassword(String passwordDigest);
}
