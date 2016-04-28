package com.digitald4.common.test;

import javax.persistence.EntityManager;

import org.junit.BeforeClass;

import com.digitald4.common.jpa.EntityManagerHelper;
import com.digitald4.common.model.User;


public class DD4TestCase {
  public static EntityManager entityManager; 
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		entityManager = EntityManagerHelper.getEntityManagerFactory("DD4JPA2", "org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/budget?autoReconnect=true", "eddiemay", "").createEntityManager();
		User.setActiveUser(entityManager.find(User.class, 1));
	}
}
