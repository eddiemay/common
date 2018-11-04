package com.digitald4.common.storage;

import com.digitald4.common.model.BasicUser;
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

public class BasicUserStore implements UserStore<BasicUser> {
	private final Provider<DAO> daoProvider;
	private final Clock clock;

	@Inject
	public BasicUserStore(Provider<DAO> daoProvider, Clock clock) {
		this.daoProvider = daoProvider;
		this.clock = clock;
	}

	@Override
	public BasicUser getType() {
		return new BasicUser();
	}

	@Override
	public BasicUser create(BasicUser user) {
		return new BasicUser(daoProvider.get().create(user.toProto()));
	}

	@Override
	public BasicUser get(long id) {
		User user = daoProvider.get().get(User.class, id);
		if (user == null) {
			return null;
		}
		return new BasicUser(user);
	}

	@Override
	public QueryResult<BasicUser> list(Query query) {
		QueryResult<User> result = daoProvider.get().list(User.class, query);
		return new QueryResult<>(
				result.getResults().stream()
						.map(BasicUser::new)
						.collect(Collectors.toList()),
				result.getTotalSize());
	}

	@Override
	public BasicUser update(long id, UnaryOperator<BasicUser> updater) {
		return new BasicUser(
				daoProvider.get().update(User.class, id, user -> updater.apply(new BasicUser(user)).toProto()));
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
	public BasicUser updateLastLogin(BasicUser user)  {
		return update(user.getId(), user_ -> user_.setLastLogin(clock.millis()));
	}

	@Override
	public BasicUser getBy(String login, String password)  {
		List<BasicUser> users = list(Query.newBuilder()
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
