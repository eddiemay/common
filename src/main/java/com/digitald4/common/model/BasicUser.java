package com.digitald4.common.model;

import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4Protos.ActiveSession;

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

	@Override
	public int getTypeId() {
		return userProto.getTypeId();
	}

	@Override
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

	@Override
	public BasicUser setLastLogin(long lastLogin) {
		userProto = userProto.toBuilder().setLastLogin(lastLogin).build();
		return this;
	}

	@Override
	public BasicUser setPasswordInfo(DD4Protos.PasswordInfo passwordInfo) {
		userProto = userProto.toBuilder().setPasswordInfo(passwordInfo).build();
		return this;
	}

	@Override
	public ActiveSession getActiveSession() {
		return activeSession;
	}

	@Override
	public BasicUser setActiveSession(ActiveSession activeSession) {
		this.activeSession = activeSession;
		return this;
	}

	@Override
	public DD4Protos.User getProto() {
		return userProto;
	}

	@Override
	public DD4Protos.User toProto() {
		return userProto;
	}

	@Override
	public BasicUser setProto(DD4Protos.User userProto) {
		this.userProto = userProto;
		return this;
	}
}