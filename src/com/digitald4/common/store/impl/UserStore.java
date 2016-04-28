package com.digitald4.common.store.impl;

import java.util.List;

import org.joda.time.DateTime;

import com.digitald4.common.dao.DAO;
import com.digitald4.common.dao.QueryParam;
import com.digitald4.common.dao.sql.DAOProtoSQLImpl;
import com.digitald4.common.distributed.Function;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4Protos.User.UserType;

public class UserStore extends GenericDAOStore<User> {
	public UserStore(DAO<User> dao) {
		super(dao);
	}
	
	public User getByUsernamePassword(String username, String password) throws DD4StorageException {
		List<User> users = query(new QueryParam("user_name", "=", username),
				new QueryParam("password_d", "=", password));
		if (users.isEmpty()) {
			return null;
		}
		return users.get(0);
	}
	
	public User getByEmailPassword(String email, String password) throws DD4StorageException {
		List<User> users = query(new QueryParam("email", "=", email),
				new QueryParam("password_d", "=", password));
		if (users.isEmpty()) {
			return null;
		}
		return users.get(0);
	}
	
	public static void main(String[] args) throws Exception {
		User user = User.newBuilder()
				.setTypeId(UserType.STANDARD)
				.setEmail("test@example.com")
				.setUserName("testuser")
				.setFirstName("Test")
				.setLastName("User")
				.setPasswordD("pass")
				.build();
		UserStore store = new UserStore(
				new DAOProtoSQLImpl<User>(user,
				new DBConnectorThreadPoolImpl("com.mysql.jdbc.Driver",
						"jdbc:mysql://localhost/budget?autoReconnect=true", "dd4_user", "getSchooled85")));
		System.out.println(user);
		try {
			user = store.create(user);
			System.out.println(user);
			System.out.println(store.read(user.getId()));
			System.out.println(store.getByUsernamePassword("testuser", "pass"));
			System.out.println(store.update(user.getId(), new Function<User, User>() {
				@Override public User execute(User user) {
					return user.toBuilder()
							.setReadOnly(true)
							.setLastLogin(DateTime.now().getMillis())
							.build();
				}
			}));
		} catch (Exception e) {
			e.printStackTrace();
		}
		store.delete(user.getId());
	}
}
