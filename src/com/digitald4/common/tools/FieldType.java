package com.digitald4.common.tools;

import java.sql.Clob;
import java.sql.Time;
import java.util.Date;
import java.sql.Types;

import org.joda.time.DateTime;

public enum FieldType {
	BOOLEAN(boolean.class, "Boolean.valueOf", "NUMBER(1)", "BIT"),
	SHORT(short.class, "Short.valueOf", "NUMBER(5)", "SMALLINT"),
	INT(int.class, "Integer.valueOf", "NUMBER(9)", "INT"),
	ID(Integer.class, "Integer.valueOf", "NUMBER(9)", "INT"),
	LONG(long.class, "Long.valueOf", "NUMBER(19)", "BIGINT"),
	DOUBLE(double.class, "Double.valueOf", "FLOAT(24)", "DOUBLE"),
	DATE(Date.class, "FormatText.parseDate", "DATE", "DATE"),
	DATETIME(DateTime.class, "new DateTime", "DATE", "DATETIME"),
	TIME(Time.class, "FormatText.parseTime", "TIME", "TIME"),
	STRING(String.class, "String.valueOf", "VARCHAR2(%s)", "VARCHAR(%s)"),
	BLOB(byte[].class, null, "BLOB", "BLOB"),
	MEDIUMBLOB(byte[].class, null, "BLOB", "MEDIUMBLOB"),
	CLOB(Clob.class, null, "CLOB", "TEXT");
	
	public enum DataStore {
		ORACLE,
		MYSQL
	}
	
	private final Class<?> javaClass;
	private final String parseCode;
	private final String oracleType;
	private final String mysqlType;
	
	private FieldType(Class<?> javaClass, String parseCode, String oracleType, String mysqlType) {
		this.javaClass = javaClass;
		this.parseCode = parseCode;
		this.oracleType = oracleType;
		this.mysqlType = mysqlType;
	}
	
	public Class<?> getJavaClass(){
		return javaClass;
	}
	
	public String getParseCode(){
		return parseCode;
	}
	
	public String getDataStoreType(DataStore ds) {
		switch(ds) {
			case ORACLE: return getOracleType();
			case MYSQL: return getMysqlType();
		}
		return null;
	}
	
	public String getOracleType(){
		return oracleType;
	}
	
	public String getMysqlType() {
		return mysqlType;
	}

	public static FieldType getColumnTypeFromDB(String colName, int type, int columnSize, int decimalDigits) {
		if(type == Types.DECIMAL && decimalDigits > 0)
			return DOUBLE;
		else if(type == Types.DECIMAL && columnSize == 1)
			return BOOLEAN;
		else if(type == Types.DECIMAL && columnSize > 9)
			return LONG;
		else if(type == Types.DECIMAL && colName.endsWith("ID"))
			return ID;
		else if(type == Types.DECIMAL)
			return INT;
		else if(type==Types.DATE)
			return DATE;
		else if(type == Types.TIME || type==Types.TIMESTAMP)
			return DATETIME;
		else if(type == Types.VARCHAR)
			return STRING;
		else if(type == Types.BLOB)
			return BLOB;
		else if(type == Types.CLOB)
			return CLOB;
		return null;
	}
}
