package com.digitald4.common.model;

import com.google.api.server.spi.config.ApiResourceProperty;

import java.time.Clock;

public interface User {

	long getId();

	User setId(long id);

	String getUsername();

	User setUsername(String username);

	String getEmail();

	User setEmail(String email);

	int getTypeId();

	User setTypeId(int typeId);

	String getFirstName();

	User setFirstName(String firstName);

	String getLastName();

	User setLastName(String lastName);

	@ApiResourceProperty
	String fullName();
}
