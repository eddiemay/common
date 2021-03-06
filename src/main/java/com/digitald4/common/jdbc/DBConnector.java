package com.digitald4.common.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class defines getConnection
 * and releaseConnection methods. In getConnection method, connection
 * caching feature of JDBC 2.0 is implemented to get the database connection.
 */
public interface DBConnector {
	Connection getConnection() throws SQLException;

	DBConnector connect(String dbDriver, String url, String user, String password);
}
