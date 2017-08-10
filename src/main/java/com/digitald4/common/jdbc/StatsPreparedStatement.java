package com.digitald4.common.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class StatsPreparedStatement implements Comparable<Object>, PreparedStatement, StatsSQL {
	private StatsConnection con;
	private PreparedStatement ps;
	private String sql;
	private long initTime;
	private long executionStartTime;
	private long executionEndTime;
	private long closeTime;

	public StatsPreparedStatement(StatsConnection con, PreparedStatement ps, String sql) {
		this.con = con;
		this.sql = sql;
		this.ps = ps;
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
		return getExecutionEndTime()-getExecutionStartTime();
	}
	
	public long getCloseTime(){
		return closeTime;
	}
	
	public long getResultSetTime(){
		return getCloseTime()-getExecutionEndTime();
	}
	
	public long getTotalTime(){
		return getCloseTime()-getInitTime();
	}

	public void addBatch() throws SQLException {
		ps.addBatch();
	}

	public void clearParameters() throws SQLException {
		ps.clearParameters();
	}
	
	public boolean execute() throws SQLException {
		executionStartTime = System.currentTimeMillis();
		boolean ret = ps.execute();
		executionEndTime = System.currentTimeMillis();
		return ret;
	}

	public ResultSet executeQuery() throws SQLException {
		executionStartTime = System.currentTimeMillis();
		ResultSet rs = ps.executeQuery();
		executionEndTime = System.currentTimeMillis();
		return rs;
	}

	public int executeUpdate() throws SQLException {
		executionStartTime = System.currentTimeMillis();
		int ret = ps.executeUpdate();
		executionEndTime = System.currentTimeMillis();
		return ret;
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return ps.getMetaData();
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		return ps.getParameterMetaData();
	}

	public void setArray(int parameterIndex, Array x) throws SQLException {
		ps.setArray(parameterIndex, x);
	}

	public void setAsciiStream(int parameterIndex, InputStream x)throws SQLException {
		ps.setAsciiStream(parameterIndex, x);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length)throws SQLException {
		ps.setAsciiStream(parameterIndex, x, length);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, long length)throws SQLException {
		ps.setAsciiStream(parameterIndex, x, length);
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x)throws SQLException {
		ps.setBigDecimal(parameterIndex, x);
	}

	public void setBinaryStream(int parameterIndex, InputStream x)throws SQLException {
		ps.setBinaryStream(parameterIndex, x);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length)throws SQLException {
		ps.setBinaryStream(parameterIndex, x, length);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, long length)throws SQLException {
		ps.setBinaryStream(parameterIndex, x, length);
	}

	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		ps.setBlob(parameterIndex, x);
	}

	public void setBlob(int parameterIndex, InputStream inputStream)throws SQLException {
		ps.setBlob(parameterIndex, inputStream);
	}

	public void setBlob(int parameterIndex, InputStream inputStream, long length)throws SQLException {
		ps.setBlob(parameterIndex, inputStream, length);
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		ps.setBoolean(parameterIndex, x);
	}

	public void setByte(int parameterIndex, byte x) throws SQLException {
		ps.setByte(parameterIndex, x);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		ps.setBytes(parameterIndex, x);
	}

	public void setCharacterStream(int parameterIndex, Reader reader)throws SQLException {
		ps.setCharacterStream(parameterIndex, reader);
	}

	public void setCharacterStream(int parameterIndex, Reader reader, int length)throws SQLException {
		ps.setCharacterStream(parameterIndex, reader, length);
	}

	public void setCharacterStream(int parameterIndex, Reader reader,long length) throws SQLException {
		ps.setCharacterStream(parameterIndex, reader, length);
	}

	public void setClob(int parameterIndex, Clob x) throws SQLException {
		ps.setClob(parameterIndex, x);
	}

	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		ps.setClob(parameterIndex, reader);
	}

	public void setClob(int parameterIndex, Reader reader, long length)throws SQLException {
		ps.setClob(parameterIndex, reader, length);
	}

	public void setDate(int parameterIndex, Date x) throws SQLException {
		ps.setDate(parameterIndex, x);
	}

	public void setDate(int parameterIndex, Date x, Calendar cal)throws SQLException {
		ps.setDate(parameterIndex, x, cal);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		ps.setDouble(parameterIndex, x);
	}

	public void setFloat(int parameterIndex, float x) throws SQLException {
		ps.setFloat(parameterIndex, x);
	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		ps.setInt(parameterIndex, x);
	}

	public void setLong(int parameterIndex, long x) throws SQLException {
		ps.setLong(parameterIndex, x);
	}

	public void setNCharacterStream(int parameterIndex, Reader value)throws SQLException {
		ps.setNCharacterStream(parameterIndex, value);
	}

	public void setNCharacterStream(int parameterIndex, Reader value,long length) throws SQLException {
		ps.setNCharacterStream(parameterIndex, value, length);
	}

	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		ps.setNClob(parameterIndex, value);
	}

	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		ps.setNClob(parameterIndex, reader);
	}

	public void setNClob(int parameterIndex, Reader reader, long length)throws SQLException {
		ps.setNClob(parameterIndex, reader, length);
	}

	public void setNString(int parameterIndex, String value)throws SQLException {
		ps.setNString(parameterIndex, value);
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		ps.setNull(parameterIndex, sqlType);
	}

	public void setNull(int parameterIndex, int sqlType, String typeName)throws SQLException {
		ps.setNull(parameterIndex, sqlType, typeName);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		ps.setObject(parameterIndex, x);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType)throws SQLException {
		ps.setObject(parameterIndex, x, targetSqlType);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType,int scaleOrLength) throws SQLException {
		ps.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
	}

	public void setRef(int parameterIndex, Ref x) throws SQLException {
		ps.setRef(parameterIndex, x);
	}

	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		ps.setRowId(parameterIndex, x);
	}

	public void setSQLXML(int parameterIndex, SQLXML xmlObject)throws SQLException {
		ps.setSQLXML(parameterIndex, xmlObject);
	}

	public void setShort(int parameterIndex, short x) throws SQLException {
		ps.setShort(parameterIndex, x);
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		ps.setString(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		ps.setTime(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x, Calendar cal)throws SQLException {
		ps.setTime(parameterIndex, x, cal);
	}

	public void setTimestamp(int parameterIndex, Timestamp x)throws SQLException {
		ps.setTimestamp(parameterIndex, x);
	}

	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)throws SQLException {
		ps.setTimestamp(parameterIndex, x, cal);
	}

	public void setURL(int parameterIndex, URL x) throws SQLException {
		ps.setURL(parameterIndex, x);
	}

	@SuppressWarnings("deprecation")
	public void setUnicodeStream(int parameterIndex, InputStream x, int length)throws SQLException {
		ps.setUnicodeStream(parameterIndex, x, length);
	}

	public void addBatch(String sql) throws SQLException {
		ps.addBatch(sql);
	}

	public void cancel() throws SQLException {
		ps.cancel();
	}

	public void clearBatch() throws SQLException {
		ps.clearBatch();
	}

	public void clearWarnings() throws SQLException {
		ps.clearWarnings();
	}

	public void close() throws SQLException {
		ps.close();
		closeTime = System.currentTimeMillis();
		con.addStatement(this);
	}

	public boolean execute(String sql) throws SQLException {
		return ps.execute(sql);
	}

	public boolean execute(String sql, int autoGeneratedKeys)throws SQLException {
		return ps.execute(sql, autoGeneratedKeys);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return ps.execute(sql, columnIndexes);
	}

	public boolean execute(String sql, String[] columnNames)throws SQLException {
		return ps.execute(sql, columnNames);
	}

	public int[] executeBatch() throws SQLException {
		return ps.executeBatch();
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		return ps.executeQuery(sql);
	}

	public int executeUpdate(String sql) throws SQLException {
		return ps.executeUpdate(sql);
	}

	public int executeUpdate(String sql, int autoGeneratedKeys)throws SQLException {
		return ps.executeUpdate(sql, autoGeneratedKeys);
	}

	public int executeUpdate(String sql, int[] columnIndexes)throws SQLException {
		return ps.executeUpdate(sql, columnIndexes);
	}

	public int executeUpdate(String sql, String[] columnNames)throws SQLException {
		return ps.executeUpdate(sql, columnNames);
	}

	public Connection getConnection() throws SQLException {
		return ps.getConnection();
	}

	public int getFetchDirection() throws SQLException {
		return ps.getFetchDirection();
	}

	public int getFetchSize() throws SQLException {
		return ps.getFetchSize();
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return ps.getGeneratedKeys();
	}

	public int getMaxFieldSize() throws SQLException {
		return ps.getMaxFieldSize();
	}

	public int getMaxRows() throws SQLException {
		return ps.getMaxRows();
	}

	public boolean getMoreResults() throws SQLException {
		return ps.getMoreResults();
	}

	public boolean getMoreResults(int current) throws SQLException {
		return ps.getMoreResults(current);
	}

	public int getQueryTimeout() throws SQLException {
		return ps.getQueryTimeout();
	}

	public ResultSet getResultSet() throws SQLException {
		return ps.getResultSet();
	}

	public int getResultSetConcurrency() throws SQLException {
		return ps.getResultSetConcurrency();
	}

	public int getResultSetHoldability() throws SQLException {
		return ps.getResultSetHoldability();
	}

	public int getResultSetType() throws SQLException {
		return ps.getResultSetType();
	}

	public int getUpdateCount() throws SQLException {
		return ps.getUpdateCount();
	}

	public SQLWarning getWarnings() throws SQLException {
		return ps.getWarnings();
	}

	public boolean isClosed() throws SQLException {
		return ps.isClosed();
	}

	public boolean isPoolable() throws SQLException {
		return ps.isPoolable();
	}

	public void setCursorName(String name) throws SQLException {
		ps.setCursorName(name);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		ps.setEscapeProcessing(enable);
	}

	public void setFetchDirection(int direction) throws SQLException {
		ps.setFetchDirection(direction);
	}

	public void setFetchSize(int rows) throws SQLException {
		ps.setFetchSize(rows);
	}

	public void setMaxFieldSize(int max) throws SQLException {
		ps.setMaxFieldSize(max);
	}

	public void setMaxRows(int max) throws SQLException {
		ps.setMaxRows(max);
	}

	public void setPoolable(boolean poolable) throws SQLException {
		ps.setPoolable(poolable);
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		ps.setQueryTimeout(seconds);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return ps.isWrapperFor(iface);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return ps.unwrap(iface);
	}
	
	public String toString(){
		return getSQL();
	}

	public String getHashKey() {
		return getSQL();
	}

	public int compareTo(Object o) {
		if(o instanceof StatsPreparedStatement){
			StatsPreparedStatement ps = (StatsPreparedStatement)o;
			if(getTotalTime() < ps.getTotalTime())
				return -1;
			if(getTotalTime() > ps.getTotalTime())
				return 1;
			if(getExecutionTime() < ps.getExecutionTime())
				return -1;
			if(getExecutionTime() > ps.getExecutionTime())
				return 1;
			if(getInitTime() < ps.getInitTime())
				return -1;
			if(getInitTime() > ps.getInitTime())
				return 1;
		}
		return toString().compareTo(o.toString());
	}
	/**
	 * Required for Java 7
	 * @throws SQLException  
	 */
	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Required for Java 7
	 * @throws SQLException  
	 */
	public boolean isCloseOnCompletion() throws SQLException {
		return false;// TODO Auto-generated method stub
	}
}
