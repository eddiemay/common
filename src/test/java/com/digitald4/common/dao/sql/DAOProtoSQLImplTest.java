package com.digitald4.common.dao.sql;

import static org.junit.Assert.*;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.Query;
import com.digitald4.common.proto.DD4Protos.Query.Filter;
import com.digitald4.common.proto.DD4Protos.User;
import com.digitald4.common.storage.DAOSQLImpl;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

public class DAOProtoSQLImplTest {

	@Test @Ignore
	public void testDateQueryWithDatabase() {
		DAOSQLImpl dao = new DAOSQLImpl(new DBConnectorThreadPoolImpl("com.mysql.jdbc.Driver",
						"jdbc:mysql://localhost/cpr?autoReconnect=true", "dd4_user", "getSchooled85"));
		User user = User.newBuilder()
				.setUsername("testuser")
				.setTypeId(4)
				.setLastLogin(new DateTime("2005-06-18T19:30:15").getMillis())
				.build();
		try {
			user = dao.create(user);
			List<User> users = dao.list(User.class, Query.newBuilder()
					.addFilter(Filter.newBuilder()
							.setColumn("last_login")
							.setOperator(">")
							.setValue(String.valueOf(new DateTime("2005-06-02").getMillis())))
					.addFilter(Filter.newBuilder()
							.setColumn("last_login")
							.setOperator("<")
							.setValue(String.valueOf(new DateTime("2005-07-11").getMillis()))
							.build())
					.build()).getResults();
			assertEquals(1, users.size());
			DateTime lastLogin = new DateTime(users.get(0).getLastLogin());
			assertEquals(2005, lastLogin.getYear());
			assertEquals(6, lastLogin.getMonthOfYear());
			assertEquals(18, lastLogin.getDayOfMonth());
			assertEquals(19, lastLogin.getHourOfDay());
			assertEquals(30, lastLogin.getMinuteOfHour());
			assertEquals(15, lastLogin.getSecondOfMinute());
		} finally {
			dao.delete(User.class, user.getId());
		}
	}
}
