package com.digitald4.common.storage;

import com.digitald4.common.model.UserBasicImpl;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.ProtoUtil;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;

public class BasicUserStore implements UserStore<UserBasicImpl> {
	private final Provider<DAO> daoProvider;
	private final Clock clock;

	@Inject
	public BasicUserStore(Provider<DAO> daoProvider, Clock clock) {
		this.daoProvider = daoProvider;
		this.clock = clock;
	}

	@Override
	public UserBasicImpl getType() {
		return new UserBasicImpl();
	}

	@Override
	public UserBasicImpl create(UserBasicImpl user) {
		return new UserBasicImpl(daoProvider.get().create(user.toProto()));
	}

	@Override
	public UserBasicImpl get(long id) {
		User user = daoProvider.get().get(User.class, id);
		if (user == null) {
			return null;
		}
		return new UserBasicImpl(user);
	}

	@Override
	public QueryResult<UserBasicImpl> list(Query query) {
		QueryResult<User> result = daoProvider.get().list(User.class, query);
		return new QueryResult<>(
				result.getResults().stream()
						.map(UserBasicImpl::new)
						.collect(Collectors.toList()),
				result.getTotalSize());
	}

	@Override
	public UserBasicImpl update(long id, UnaryOperator<UserBasicImpl> updater) {
		return new UserBasicImpl(
				daoProvider.get().update(User.class, id, user -> updater.apply(new UserBasicImpl(user)).toProto()));
	}

	@Override
	public void delete(long id) {
		daoProvider.get().delete(User.class, id);
	}

	@Override
	public int delete(Query query) {
		return daoProvider.get().delete(User.class, query);
	}

	@Override
	public UserBasicImpl updateLastLogin(UserBasicImpl user)  {
		return update(user.getId(), user_ -> user_.setLastLogin(clock.millis()));
	}

	@Override
	public UserBasicImpl getBy(String login, String password)  {
		List<UserBasicImpl> users = list(Query.newBuilder()
				.addFilter(Filter.newBuilder()
						.setColumn(login.contains("@") ? "email" : "user_name")
						.setOperator("=")
						.setValue(login))
				.addFilter(ProtoUtil.createFilter("password", "=", encodePassword(password)))
				.build())
				.getResults();
		if (users.isEmpty()) {
			return null;
		}
		return users.get(0);
	}

	private static String encodePassword(String password) {
		try {
			return Calculate.md5(password);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae);
		}
	}
}
