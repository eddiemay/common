package com.digitald4.common.storage;

import java.util.List;

import org.joda.time.DateTime;

import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4Protos.User.UserType;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.QueryParam;

public class UserStore extends GenericDAOStore<User> {
	public UserStore(DAO<User> dao) {
		super(dao);
	}
	
	public User getByUsernamePassword(String username, String password) throws DD4StorageException {
		List<User> users = get(
				QueryParam.newBuilder()
						.setColumn("user_name")
						.setOperan("=")
						.setValue(username)
						.build(),
				QueryParam.newBuilder()
						.setColumn("password")
						.setOperan("=")
						.setValue(password)
						.build());
		if (users.isEmpty()) {
			return null;
		}
		return users.get(0);
	}
	
	public User getByEmailPassword(String email, String password) throws DD4StorageException {
		List<User> users = get(
				QueryParam.newBuilder()
				.setColumn("email")
				.setOperan("=")
				.setValue(email)
				.build(),
		QueryParam.newBuilder()
				.setColumn("password")
				.setOperan("=")
				.setValue(password)
				.build());
		if (users.isEmpty()) {
			return null;
		}
		return users.get(0);
	}

	public User updateLastLogin(User user) throws DD4StorageException {
		return update(user.getId(), new Function<User, User>() {
			@Override
			public User execute(User user) {
				return user.toBuilder()
						.setLastLogin(DateTime.now().getMillis())
						.build();
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		User user = User.newBuilder()
				.setType(UserType.STANDARD)
				.setEmail("test@example.com")
				.setUserName("testuser")
				.setFirstName("Test")
				.setLastName("User")
				.setPassword("pass")
				.build();
		UserStore store = new UserStore(
				new DAOProtoSQLImpl<>(User.class,
						new DBConnectorThreadPoolImpl("com.mysql.jdbc.Driver",
								"jdbc:mysql://localhost/cpr?autoReconnect=true", "dd4_user", "getSchooled85")));
		System.out.println(user);
		try {
			System.out.println(user = store.create(user));
			System.out.println(store.get(user.getId()));
			System.out.println(store.getByUsernamePassword("testuser", "pass"));
			System.out.println(store.update(user.getId(), new Function<User, User>() {
				@Override public User execute(User user) {
					return user.toBuilder()
							.setReadOnly(true)
							.setLastLogin(DateTime.now().getMillis())
							.build();
				}
			}));
			System.out.println(store.updateLastLogin(user));
		} catch (Exception e) {
			e.printStackTrace();
		}
		store.delete(user.getId());
	}
}
