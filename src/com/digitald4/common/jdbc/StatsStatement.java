package com.digitald4.common.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class StatsStatement implements Statement,StatsSQL {
	
	private StatsConnection con;
	private Statement statement;
	private String sql;
	private long initTime;
	private long executionStartTime;
	private long executionEndTime;
	private long closeTime;
	
	public StatsStatement(StatsConnection con, Statement statement){
		this.con = con;
		this.statement = statement;
		initTime = System.currentTimeMillis();
	}
	
	public String getSQL(){
		return sql;
	}
	
	public long getInitTime(){
		return initTime;
	}
	
	public long getExecutionStartTime(){
		return executionStartTime;
	}
	
	public long getExecutionEndTime(){
		return executionEndTime;
	}
	
	public long getExecutionTime(){
		return getExecutionEndTime() - getExecutionStartTime();
	}
	
	public long getCloseTime(){
		return closeTime;
	}
	
	public long getResultSetTime(){
		return getCloseTime() - getExecutionEndTime();
	}
	
	public long getTotalTime(){
		return getCloseTime() - getInitTime();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return statement.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return statement.isWrapperFor(iface);
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		ResultSet rs = statement.executeQuery(sql);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		int rs = statement.executeUpdate(sql);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public void close() throws SQLException {
		statement.close();
		closeTime = System.currentTimeMillis();
		con.addStatement(this);
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return statement.getMaxFieldSize();
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		statement.setMaxFieldSize(max);
	}

	@Override
	public int getMaxRows() throws SQLException {
		return statement.getMaxRows();
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		statement.setMaxRows(max);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		statement.setEscapeProcessing(enable);
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return statement.getQueryTimeout();
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		statement.setQueryTimeout(seconds);
	}

	@Override
	public void cancel() throws SQLException {
		statement.cancel();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return statement.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		statement.clearWarnings();
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		statement.setCursorName(name);
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		boolean rs = statement.execute(sql);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return statement.getResultSet();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return statement.getUpdateCount();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return statement.getMoreResults();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		statement.setFetchDirection(direction);
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return statement.getFetchDirection();
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		statement.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException {
		return statement.getFetchSize();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return statement.getResultSetConcurrency();
	}

	@Override
	public int getResultSetType() throws SQLException {
		return statement.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		this.sql = sql;
		statement.addBatch(sql);
	}

	@Override
	public void clearBatch() throws SQLException {
		statement.clearBatch();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		executionStartTime = System.currentTimeMillis();
		int[] rs = statement.executeBatch();
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return statement.getConnection();
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return statement.getMoreResults();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return statement.getGeneratedKeys();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys)throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		int rs = statement.executeUpdate(sql,autoGeneratedKeys);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes)throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		int rs = statement.executeUpdate(sql,columnIndexes);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames)throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		int rs = statement.executeUpdate(sql,columnNames);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys)throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		boolean rs = statement.execute(sql,autoGeneratedKeys);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		boolean rs = statement.execute(sql,columnIndexes);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public boolean execute(String sql, String[] columnNames)throws SQLException {
		this.sql = sql;
		executionStartTime = System.currentTimeMillis();
		boolean rs = statement.execute(sql,columnNames);
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return statement.getResultSetHoldability();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return statement.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		statement.setPoolable(poolable);
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return statement.isPoolable();
	}

	/**This method is required for Java 7
	 * @throws SQLException  
	 */
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	/**This method i required for Java 7
	 * @throws SQLException  
	 */
	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
}
