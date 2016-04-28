package com.digitald4.common.jpa;

import static org.junit.Assert.*;

import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

public class PropertyCollectionFactoryTest {

	@Test
	public void test() throws Exception {
		PropertyCollectionFactory<TestClass> pcf = new PropertyCollectionFactory<TestClass>();
		assertTrue(pcf.isEmpty());
		TestClass tc3 = new TestClass().setS("str").setI(3).setB(false).setD(DateTime.now().toDate());
		TestClass tc4 = new TestClass().setS("str").setI(4).setB(true).setD(DateTime.now().minusDays(2).toDate());
		TestClass tc5 = new TestClass().setS("str").setI(5).setB(false).setD(DateTime.now().plusDays(30).toDate());
		TestClass tc6 = new TestClass().setS("str").setI(6).setB(false).setD(DateTime.now().minusDays(15).toDate());
		TestClass tc7 = new TestClass().setS("str").setI(7).setB(true).setD(DateTime.now().plusDays(7).toDate());
		DD4TypedQueryImpl<TestClass> tq = new DD4TypedQueryImpl<TestClass>(null, "test", "SELECT o FROM TestClass WHERE o.I=?1", TestClass.class);
		tq.setParameter(1, 5);
		pcf.getList(false, tq);
		assertTrue(pcf.isEmpty());
		pcf.cache(tc5, tq);
		assertFalse(pcf.isEmpty());
		assertEquals(1, pcf.getList(false, tq).size());
		pcf.cache(tc4);
		assertEquals(1, pcf.getList(false, tq).size());
		assertEquals(5, pcf.getList(false, tq).get(0).getI());
		pcf.cache(tc6);
		assertEquals(1, pcf.getList(false, tq).size());
		assertEquals(5, pcf.getList(false, tq).get(0).getI());
		
		DD4TypedQueryImpl<TestClass> tq2 = new DD4TypedQueryImpl<TestClass>(null, "test", "SELECT o FROM TestClass WHERE o.B=?1", TestClass.class);
		tq2.setParameter(1, true);
		pcf.getList(true, tq2);
		DD4TypedQueryImpl<TestClass> tq3 = new DD4TypedQueryImpl<TestClass>(null, "test", "SELECT o FROM TestClass WHERE o.B=?1", TestClass.class);
		tq3.setParameter(1, false);
		pcf.getList(true, tq3);
		assertEquals(2, pcf.getPropertyCollections().size());
		pcf.cache(tc3);
		pcf.cache(tc4);
		pcf.cache(tc5);
		pcf.cache(tc6);
		pcf.cache(tc7);
		assertEquals(1, pcf.getList(false, tq).size());
		assertEquals(5, pcf.getList(false, tq).get(0).getI());
		assertEquals(2, pcf.getList(false, tq2).size());
		assertEquals(true, pcf.getList(false, tq2).get(0).isB());
		assertEquals(3, pcf.getList(false, tq3).size());
		assertEquals(false, pcf.getList(false, tq3).get(0).isB());
		
		DD4TypedQueryImpl<TestClass> tq4 = new DD4TypedQueryImpl<TestClass>(null, "test", "SELECT o FROM TestClass WHERE o.I<?1", TestClass.class);
		tq4.setParameter(1, 6);
		pcf.getList(true, tq4);
		pcf.cache(tc3);
		pcf.cache(tc4);
		pcf.cache(tc5);
		pcf.cache(tc6);
		pcf.cache(tc7);
		for (TestClass tc : pcf.getList(false, tq4)) {
			System.out.println(tc.getI());
		}
		assertEquals(3, pcf.getList(false, tq4).size());
		for (TestClass tc : pcf.getList(false, tq4)) {
			assertTrue(tc.getI() < 6);
		}
		
		DD4TypedQueryImpl<TestClass> tq5 = new DD4TypedQueryImpl<TestClass>(null, "test", "SELECT o FROM TestClass WHERE o.I>?1", TestClass.class);
		tq5.setParameter(1, 5);
		pcf.getList(true, tq5);
		pcf.cache(tc3);
		pcf.cache(tc4);
		pcf.cache(tc5);
		pcf.cache(tc6);
		pcf.cache(tc7);
		for (TestClass tc : pcf.getList(false, tq5)) {
			System.out.println(tc.getI());
		}
		assertEquals(2, pcf.getList(false, tq5).size());
		for (TestClass tc : pcf.getList(false, tq5)) {
			assertTrue(tc.getI() > 5);
		}
		
		DateTime now = DateTime.now();
		DD4TypedQueryImpl<TestClass> tq6 = new DD4TypedQueryImpl<TestClass>(null, "test", "SELECT o FROM TestClass WHERE o.D<?1", TestClass.class);
		tq6.setParameter(1, now.minusMillis(now.getMillisOfDay()).toDate());
		pcf.getList(true, tq6);
		pcf.cache(tc3);
		pcf.cache(tc4);
		pcf.cache(tc5);
		pcf.cache(tc6);
		pcf.cache(tc7);
		for (TestClass tc : pcf.getList(false, tq6)) {
			System.out.println(tc.getD());
		}
		assertEquals(2, pcf.getList(false, tq6).size());
	}
	
	@Test @Ignore
	public void testGetExpressions() {
		String[] expr = "select e from Employee e where e.dprt = :DE AND e.salary > ?2;"
				.split("[\\w]");
		for (String e : expr) {
			System.out.println(e);
		}
		assertEquals(2, expr.length);
	}
	
	private class TestClass implements Comparable<Object>{
		private String s;
		private int i;
		private boolean b;
		private Date d;
		
		public String getS() {
			return s;
		}
		public TestClass setS(String s) {
			this.s = s;
			return this;
		}
		public int getI() {
			return i;
		}
		public TestClass setI(int i) {
			this.i = i;
			return this;
		}
		public boolean isB() {
			return b;
		}
		public TestClass setB(boolean b) {
			this.b = b;
			return this;
		}
		public Date getD() {
			return d;
		}
		public TestClass setD(Date d) {
			this.d = d;
			return this;
		}
		public String toString() {
			return getS() + getI() + isB();
		}
		@Override
		public int compareTo(Object o) {
			return toString().compareTo(o.toString());
		}
	}
}
