package com.digitald4.common.storage;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.QueryParam;
import com.digitald4.common.util.Calculate;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.UnaryOperator;

import org.joda.time.DateTime;

public class UserStore extends GenericStore<User> {
	public UserStore(DAO<User> dao) {
		super(dao);
	}
	
	public User getBy(String login, String password) throws DD4StorageException {
		List<User> users = get(
				QueryParam.newBuilder()
						.setColumn(login.contains("@") ? "email" : "user_name")
						.setOperan("=")
						.setValue(login)
						.build(),
				QueryParam.newBuilder()
						.setColumn("password")
						.setOperan("=")
						.setValue(encodePassword(password))
						.build());
		if (users.isEmpty()) {
			return null;
		}
		return users.get(0);
	}

	public User updateLastLogin(User user) throws DD4StorageException {
		return update(user.getId(), new UnaryOperator<User>() {
			@Override
			public User apply(User user) {
				return user.toBuilder()
						.setLastLogin(DateTime.now().getMillis())
						.build();
			}
		});
	}

	public static String encodePassword(String password) {
		try {
			return Calculate.md5(password);
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae);
		}
	}
}
