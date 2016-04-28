package com.digitald4.common.jdbc;

public interface Observer<DataAccessObject> {
	public void update(DataAccessObject dao);
}
