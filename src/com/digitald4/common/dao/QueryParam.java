package com.digitald4.common.dao;

public class QueryParam {
	private final String column;
	private final String operan;
	private final Object value;
	
	public QueryParam(String column, String operan, Object value) {
		this.column = column;
		this.operan = operan;
		this.value = value;
	}
	
	public String getColumn() {
		return column;
	}
	
	public String getOperan() {
		return operan;
	}
	
	public Object getValue() {
		return value;
	}
}
