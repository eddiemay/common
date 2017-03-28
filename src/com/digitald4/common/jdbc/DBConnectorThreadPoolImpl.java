package com.digitald4.common.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

public class DBConnectorThreadPoolImpl implements DBConnector {
	
	private final PoolingDataSource dataSource;
	
	public DBConnectorThreadPoolImpl(String dbDriver, String url, String user, String password) {
		//
		// First, we'll need a ObjectPool that serves as the
		// actual pool of connections.
		//
		// We'll use a GenericObjectPool instance, although
		// any ObjectPool implementation will suffice.
		//
		try {
			Class.forName(dbDriver).newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Error loading database driver", e);
		}
		GenericObjectPool<Object> connectionPool = new GenericObjectPool<Object>(null);
		connectionPool.setMaxActive(10);
		connectionPool.setMaxWait(20);
		connectionPool.setMaxIdle(2);

		//
		// Next, we'll create a ConnectionFactory that the
		// pool will use to create Connections.
		// We'll use the DriverManagerConnectionFactory,
		// using the connect string passed in the command line
		// arguments.
		//
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(url, user, password);

		//
		// Now we'll create the PoolableConnectionFactory, which wraps
		// the "real" Connections created by the ConnectionFactory with
		// the classes that implement the pooling functionality.
		//
		new PoolableConnectionFactory(connectionFactory, connectionPool,null,null,false,true);

		//
		// Finally, we create the PoolingDriver itself,
		// passing in the object pool we created.
		//
		dataSource = new PoolingDataSource(connectionPool);

	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
}
