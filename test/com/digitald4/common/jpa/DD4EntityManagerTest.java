package com.digitald4.common.jpa;

import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.TypedQuery;

import org.joda.time.DateTime;
import org.junit.Test;

import com.digitald4.common.model.GenData;
import com.digitald4.common.model.TransHist;
import com.digitald4.common.model.User;
import com.digitald4.common.test.DD4TestCase;

public class DD4EntityManagerTest extends DD4TestCase {

	
	@Test
	public void testFind() {
		User user = entityManager.find(User.class, new PrimaryKey<Integer>(1));
		assertNotNull(user);
		assertEquals("Eddie", user.getFirstName());
	}
	
	@Test
	public void testFindByQuery() {
		TypedQuery<TransHist> tq = entityManager.createQuery("SELECT o FROM TransHist o WHERE o.USER_ID=?1", TransHist.class);
		tq.setParameter(1, 1);
		List<TransHist> transactions = tq.getResultList();
		assertTrue(transactions.size() > 5);
		for (TransHist th : transactions) {
			assertSame(1, th.getUserId());
		}
		
		TypedQuery<TransHist> tq2 = entityManager.createQuery("SELECT o FROM TransHist o WHERE o.USER_ID=?1", TransHist.class);
		tq2.setParameter(1, 1);
		List<TransHist> transactions2 = tq2.getResultList();
		assertTrue(transactions == transactions2);
	}
	
	@Test
	public void testMerge() throws Exception {
		User user = entityManager.find(User.class, new PrimaryKey<Integer>(1));
		user.setLastLogin(DateTime.now());
		entityManager.merge(user);
	}
	
	@Test
	public void testRemove() throws Exception {
		TransHist th = new TransHist(entityManager).setId(42);
		entityManager.remove(th);
	}
	
	@Test
	public void testPersist() throws Exception {
		User user = entityManager.find(User.class, 1);
		int histSize = user.getTransHists().size();
		TransHist th = new TransHist(entityManager).setUserId(1).setObject(User.class.getSimpleName()).setRowId(1)
				.setType(GenData.TransType_Update.get(entityManager))
				.setData("TEST").setTimestamp(DateTime.now());
		entityManager.persist(th);
		assertEquals(histSize + 1, user.getTransHists().size());
	}
	
	@Test
	public void testRefresh() throws Exception {
		User user = entityManager.find(User.class, new PrimaryKey<Integer>(1));
		entityManager.refresh(user);
	}
}
