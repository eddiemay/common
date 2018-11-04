package com.digitald4.common.model;

import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4Protos.ActiveSession;
import com.digitald4.common.proto.DD4Protos.PasswordInfo;

public class UserBasicImpl implements User<DD4Protos.User> {
	private DD4Protos.User userProto;
	private ActiveSession activeSession;

	public UserBasicImpl() {
		this.userProto = DD4Protos.User.getDefaultInstance();
	}

	public UserBasicImpl(DD4Protos.User userProto) {
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
	public UserBasicImpl setTypeId(int typeId) {
		userProto = userProto.toBuilder().setTypeId(typeId).build();
		return this;
	}

	@Override
	public String getUsername() {
		return userProto.getUsername();
	}

	@Override
	public UserBasicImpl setUsername(String username) {
		userProto = userProto.toBuilder().setUsername(username).build();
		return this;
	}

	@Override
	public long getLastLogin() {
		return userProto.getLastLogin();
	}

	@Override
	public UserBasicImpl setLastLogin(long lastLogin) {
		userProto = userProto.toBuilder().setLastLogin(lastLogin).build();
		return this;
	}

	@Override
	public UserBasicImpl setPasswordInfo(PasswordInfo passwordInfo) {
		userProto = userProto.toBuilder().setPasswordInfo(passwordInfo).build();
		return this;
	}

	@Override
	public ActiveSession getActiveSession() {
		return activeSession;
	}

	@Override
	public UserBasicImpl setActiveSession(ActiveSession activeSession) {
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
	public UserBasicImpl setProto(DD4Protos.User userProto) {
		this.userProto = userProto;
		return this;
	}
}