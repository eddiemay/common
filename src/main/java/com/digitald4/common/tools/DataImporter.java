package com.digitald4.common.tools;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.model.GeneralData;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.*;
import com.google.common.collect.ImmutableList;
import java.io.IOException;

public class DataImporter {
	private final DAOApiImpl apiDAO;
	private final DAO dao;

	public DataImporter(DAOApiImpl apiDAO, DAO dao) {
		this.apiDAO = apiDAO;
		this.dao = dao;
	}

	public <T> void runFor(Class<T> c) throws IOException {
		runFor(c, Query.forList());
	}

	public <T> void runFor(Class<T> c, Query.List listQuery) throws IOException {
		ImmutableList<T> results = dao.list(c, listQuery).getItems();
		results.stream().parallel().forEach(t -> {
			try {
				apiDAO.create(t);
			} catch (Exception ioe) {
				ioe.printStackTrace();
			}
		});
	}

	public <T> QueryResult<T> export(Class<T> c) {
		return apiDAO.list(c, Query.forList());
	}

	public static void main(String[] args) throws IOException {
		DataImporter dataImporter = new DataImporter(
				new DAOApiImpl(new APIConnector("\"https://ip360-179401.appspot.com/api\"", null).login()),
				new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
						"jdbc:mysql://localhost/iisosnet_main?autoReconnect=true",
						"dd4_user", "getSchooled85")));

		// dataImporter.runFor(GeneralData.class);
		dataImporter.export(GeneralData.class)
				.getItems()
				.forEach(System.out::println);
	}
}
