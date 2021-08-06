package com.digitald4.common.storage;

import com.digitald4.common.model.BasicUser;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import javax.inject.Provider;

public class BasicUserStore extends GenericStore<BasicUser> implements UserStore<BasicUser> {
	private final BasicUser type = new BasicUser();

	@Inject
	public BasicUserStore(Provider<DAO> daoProvider) {
		super(BasicUser.class, daoProvider);
	}

	@Override
	public BasicUser getType() {
		return type;
	}

	@Override
	public BasicUser getBy(String username)  {
		ImmutableList<BasicUser> users = list(
				Query.forValues((username.contains("@") ? "email" : "username") + "=" + username, null, 0, 0))
				.getResults();
		if (users.isEmpty()) {
			return null;
		}

		return users.get(0);
	}
}
