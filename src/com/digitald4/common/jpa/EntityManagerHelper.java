package com.digitald4.common.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Persistence;
import javax.persistence.Table;

public class EntityManagerHelper {

	public static EntityManagerFactory getEntityManagerFactory(String dbDriver, String url, String username, String password) throws Exception {
		return getEntityManagerFactory("DD4JPA2", dbDriver, url, username, password);
	}
	
	public static EntityManagerFactory getEntityManagerFactory(String persistenceUnit, String dbDriver, String dbUrl, String username, String password) throws Exception{
		HashMap<String,Object> properties = new HashMap<String,Object>();
//		properties.put(PersistenceUnitProperties.TRANSACTION_TYPE, PersistenceUnitTransactionType.RESOURCE_LOCAL.name());
		properties.put("javax.persistence.jdbc.driver", dbDriver);
		properties.put("javax.persistence.jdbc.url", dbUrl);
		properties.put("javax.persistence.jdbc.user", username);
		properties.put("javax.persistence.jdbc.password", password);
		return getEntityManagerFactory(persistenceUnit, properties);
	}
	
	public static EntityManagerFactory getEntityManagerFactory(String persistenceUnit, Map<String, Object> map) throws Exception {
		if (persistenceUnit.equals("DD4JPA")) {
			return new DD4EntityManagerFactory(map);
		} else if (persistenceUnit.equals("DD4JPA2")) {
			map.put("version", "2");
			return new DD4EntityManagerFactory(map);
		} else {
			return Persistence.createEntityManagerFactory(persistenceUnit, map);
		}
	}
	
	public static <T> String getNamedQuery(String namedQuery, Class<T> c) {
		NamedQueries namedQueries = c.getAnnotation(NamedQueries.class);
		if (namedQueries == null) {
			return null;
		}
		for (NamedQuery nq : namedQueries.value()) {
			if (nq.name().equalsIgnoreCase(namedQuery)) {
				return nq.query();
			}
		}
		return null;
	}
	
	public static <T> String getNamedNativeQuery(String name, Class<T> c) {
		NamedNativeQueries namedQueries = c.getAnnotation(NamedNativeQueries.class);
		if (namedQueries == null) {
			return null;
		}
		for (NamedNativeQuery nq : namedQueries.value()) {
			if (nq.name().equalsIgnoreCase(name)) {
				return nq.query();
			}
		}
		return null;
	}
	
	public static <T> String convertJPQL2SQL(Class<T> c, String query) {
		System.out.println("Converting query: " + query);
		String cq = query.replaceFirst("o", "o.*");
		cq = cq.replaceFirst(c.getSimpleName(), c.getAnnotation(Table.class).name());
		for (int x = 1; x < 10; x++) {
			cq = cq.replace("?" + x, "?");
		}
		return cq;
	}
}