package com.digitald4.common.storage;

import com.digitald4.common.model.User;
import javax.inject.Inject;
import javax.inject.Provider;

public class GenericUserStore<U extends User> extends GenericStore<U, Long> implements UserStore<U> {
	@Inject
	public GenericUserStore(U type, Provider<DAO> daoProvider) {
		super(type, daoProvider);
	}

	public GenericUserStore(Class<U> cls, Provider<DAO> daoProvider) {
		super(cls, daoProvider);
	}
}
