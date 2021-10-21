package com.digitald4.common.model;

import com.google.api.server.spi.config.ApiResourceProperty;

public class BasicUser implements User {
	private long id;
	private int typeId;
	private String username;
	private String email;
	private String firstName;
	private String lastName;

	@Override
	public long getId() {
		return id;
	}

	@Override
	public BasicUser setId(long id) {
		this.id = id;
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
	public String getUsername() {
		return username;
	}

	@Override
	public BasicUser setUsername(String username) {
		this.username = username;
		return this;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public BasicUser setEmail(String email) {
		this.email = email;
		return this;
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	@Override
	public BasicUser setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	@Override
	public String getLastName() {
		return lastName;
	}

	@Override
	public BasicUser setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	@Override
	@ApiResourceProperty
	public String fullName() {
		return String.format("%s %s", getFirstName(), getLastName());
	}
}