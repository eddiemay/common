package com.digitald4.common.model;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4Protos.ActiveSession;
import java.time.Clock;

public class BasicUser implements User<DD4Protos.User, DD4Protos.PasswordInfo> {
	private DD4Protos.User userProto;
	private ActiveSession activeSession;

	public BasicUser() {
		this.userProto = DD4Protos.User.getDefaultInstance();
	}

	public BasicUser(DD4Protos.User userProto) {
		this.userProto = userProto;
	}

	@Override
	public long getId() {
		return userProto.getId();
	}

	public BasicUser setId(long id) {
		userProto = userProto.toBuilder().setId(id).build();
		return this;
	}

	@Override
	public int getTypeId() {
		return userProto.getTypeId();
	}

	public BasicUser setTypeId(int typeId) {
		userProto = userProto.toBuilder().setTypeId(typeId).build();
		return this;
	}

	@Override
	public String getUsername() {
		return userProto.getUsername();
	}

	@Override
	public BasicUser setUsername(String username) {
		userProto = userProto.toBuilder().setUsername(username).build();
		return this;
	}

	@Override
	public long getLastLogin() {
		return userProto.getLastLogin();
	}

	public BasicUser setLastLogin(long lastLogin) {
		userProto = userProto.toBuilder().setLastLogin(lastLogin).build();
		return this;
	}

	@Override
	public BasicUser updateLastLogin(Clock clock) {
		return setLastLogin(clock.millis());
	}

	@Override
	public BasicUser updatePasswordInfo(DD4Protos.PasswordInfo passwordInfo) {
		userProto = userProto.toBuilder().setPasswordInfo(passwordInfo).build();
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
	public DD4Protos.User toProto() {
		return userProto;
	}

	public BasicUser fromProto(DD4Protos.User userProto) {
		this.userProto = userProto;
		return this;
	}

	@Override
	public void verifyPassword(String passwordDigest) {
		if (!userProto.getPasswordInfo().getPasswordDigest().equals(passwordDigest)) {
			throw new DD4StorageException("Wrong username or password", 401);
		}
	}
}