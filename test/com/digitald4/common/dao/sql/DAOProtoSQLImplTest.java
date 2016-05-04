package com.digitald4.common.dao.sql;

import static org.junit.Assert.*;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import com.digitald4.common.dao.QueryParam;
import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.proto.DD4Protos.User;

public class DAOProtoSQLImplTest {

	@Test
	public void testDateQueryWithDatabase() throws Exception {
		DAOProtoSQLImpl<User> dao = new DAOProtoSQLImpl<>(
				User.getDefaultInstance(),
				new DBConnectorThreadPoolImpl("com.mysql.jdbc.Driver",
						"jdbc:mysql://localhost/cpr?autoReconnect=true", "dd4_user", "getSchooled85"));
		List<User> users = dao.query(new QueryParam("last_login", ">", new DateTime("2016-05-01").getMillis()),
				new QueryParam("last_login", "<", new DateTime("2016-05-04").getMillis()));
		assertTrue(users.size() > 0);
	}
}
