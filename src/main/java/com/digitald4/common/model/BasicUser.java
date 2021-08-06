package com.digitald4.common.model;

import com.digitald4.common.exception.DD4StorageException;
import java.time.Clock;

public class BasicUser implements User {
	private long id;
	private String username;
	private int typeId;
	private long lastLogin;
	private PasswordInfo passwordInfo;
	private ActiveSession activeSession;

	@Override
	public long getId() {
		return id;
	}

	public BasicUser setId(long id) {
		this.id = id;
		return this;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public BasicUser setUsername(String username) {
		this.username = username;
		return this;
	}

	public int getTypeId() {
		return typeId;
	}

	public BasicUser setTypeId(int typeId) {
		this.typeId = typeId;
		return this;
	}

	@Override
	public long getLastLogin() {
		return lastLogin;
	}

	public BasicUser setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
		return this;
	}

	@Override
	public BasicUser updateLastLogin(Clock clock) {
		return setLastLogin(clock.millis());
	}

	@Override
	public BasicUser updatePasswordInfo(PasswordInfo passwordInfo) {
		this.passwordInfo = passwordInfo;
		return this;
	}

	@Override
	public ActiveSession activeSession() {
		return activeSession;
	}

	@Override
	public BasicUser activeSession(ActiveSession activeSession) {
		this.activeSession = activeSession;
		return this;
	}

	@Override
	public void verifyPassword(String passwordDigest) {
		if (!passwordInfo.getDigest().equals(passwordDigest)) {
			throw new DD4StorageException("Wrong username or password", 401);
		}
	}
}