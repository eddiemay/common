package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;
import com.digitald4.common.util.Calculate;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class UserStore extends GenericStore<User> {
	private final Clock clock;

	public UserStore(DAO<User> dao, Clock clock) {
		super(dao);
		this.clock = clock;
	}

	@Override
	public User create(User user) {
		return super.create(updateCalculated.apply(user.toBuilder())
				.setPassword(encodePassword(user.getPassword())).build())
				.toBuilder().clearPassword().build();
	}

	@Override
	public User get(long id) {
		User user = super.get(id);
		if (user == null) {
			return null;
		}
		return user.toBuilder().clearPassword().build();
	}

	@Override
	public ListResponse<User> list(ListRequest request) {
		ListResponse<User> result = super.list(request);
		return result.toBuilder()
				.setResultList(result.getResultList().stream()
						.map(user -> user.toBuilder().clearPassword().build())
						.collect(Collectors.toList()))
				.build();
	}

	@Override
	public User update(long id, UnaryOperator<User> updater) {
		return super.update(id, user -> updateCalculated.apply(updater.apply(user).toBuilder()).build())
				.toBuilder().clearPassword().build();
	}

	public User updateLastLogin(User user) throws DD4StorageException {
		return update(user.getId(), user_ -> user_.toBuilder()
				.setLastLogin(clock.millis())
				.build());
	}

	public User getBy(String login, String password) throws DD4StorageException {
		List<User> users = list(
				ListRequest.newBuilder()
						.addFilter(Filter.newBuilder()
								.setColumn(login.contains("@") ? "email" : "user_name")
								.setOperan("=")
								.setValue(login))
						.addFilter(Filter.newBuilder()
								.setColumn("password")
								.setOperan("=")
								.setValue(encodePassword(password)))
						.build())
				.getResultList();
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

	private static final UnaryOperator<User.Builder> updateCalculated = user -> user
			.setFullName(user.getFirstName() + " " + user.getLastName());
}
