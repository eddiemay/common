package com.digitald4.common.dao.sql;

import static org.junit.Assert.*;

import com.digitald4.common.proto.DD4UIProtos.ListRequest;
import java.util.List;

import com.digitald4.common.storage.DAOProtoSQLImpl;
import org.joda.time.DateTime;
import org.junit.Test;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.proto.DD4Protos.User.UserType;
import com.digitald4.common.proto.DD4UIProtos.ListRequest.Filter;

public class DAOProtoSQLImplTest {

	@Test
	public void testDateQueryWithDatabase() throws Exception {
		DAOProtoSQLImpl<User> dao = new DAOProtoSQLImpl<>(User.class,
				new DBConnectorThreadPoolImpl("com.mysql.jdbc.Driver",
						"jdbc:mysql://localhost/cpr?autoReconnect=true", "dd4_user", "getSchooled85"));
		User user = User.newBuilder()
				.setFirstName("Test")
				.setLastName("User")
				.setUserName("testuser")
				.setEmail("test@example.com")
				.setType(UserType.STANDARD)
				.setLastLogin(new DateTime("2005-06-18T19:30:15").getMillis())
				.build();
		try {
			user = dao.create(user);
			List<User> users = dao.list(ListRequest.newBuilder()
					.addFilter(Filter.newBuilder()
							.setColumn("last_login")
							.setOperan(">")
							.setValue(String.valueOf(new DateTime("2005-06-02").getMillis())))
					.addFilter(Filter.newBuilder()
							.setColumn("last_login")
							.setOperan("<")
							.setValue(String.valueOf(new DateTime("2005-07-11").getMillis()))
							.build())
					.build()).getResultList();
			assertEquals(1, users.size());
			DateTime lastLogin = new DateTime(users.get(0).getLastLogin());
			assertEquals(2005, lastLogin.getYear());
			assertEquals(6, lastLogin.getMonthOfYear());
			assertEquals(18, lastLogin.getDayOfMonth());
			assertEquals(19, lastLogin.getHourOfDay());
			assertEquals(30, lastLogin.getMinuteOfHour());
			assertEquals(15, lastLogin.getSecondOfMinute());
		} finally {
			dao.delete(user.getId());
		}
	}
}
