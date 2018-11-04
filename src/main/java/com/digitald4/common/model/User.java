package com.digitald4.common.model;

import com.digitald4.common.proto.DD4Protos;
import com.digitald4.common.proto.DD4Protos.ActiveSession;
import com.digitald4.common.proto.DD4Protos.PasswordInfo;
import com.google.protobuf.Message;

public interface User<P extends Message> extends HasProto<P> {

	long getId();

	int getTypeId();

	User setTypeId(int typeId);

	String getUsername();

	User setUsername(String username);

	long getLastLogin();

	User setLastLogin(long lastLogin);

	User setPasswordInfo(PasswordInfo passwordInfo);

	ActiveSession getActiveSession();

	User setActiveSession(ActiveSession activeSession);
}
