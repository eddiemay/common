package com.digitald4.common.component;

import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Test;

public class NotificationTest {

	@Test
	public void test() {
		Notification<Object> notification = new Notification<Object>("Payment Due",
				DateTime.now().plusDays(25).toDate(), Notification.Type.ERROR, null);
		assertEquals("Payment Due", notification.getTitle());
		assertEquals(Notification.Type.ERROR, notification.getType());
		assertTrue(notification.isBetween(DateTime.now().toDate(), DateTime.now().plusDays(30).toDate()));
		assertFalse(notification.isBetween(DateTime.now().toDate(), DateTime.now().plusDays(10).toDate()));
		assertFalse(notification.isBetween(DateTime.now().plusDays(30).toDate(), DateTime.now().plusDays(40).toDate()));
		
		notification = new Notification<Object>("Close Escrow",
				DateTime.now().plusDays(30).toDate(), Notification.Type.INFO, null);
		assertEquals("Close Escrow", notification.getTitle());
		assertEquals(Notification.Type.INFO, notification.getType());
		assertTrue(notification.isBetween(DateTime.now().toDate(), DateTime.now().plusDays(30).toDate()));
		assertFalse(notification.isBetween(DateTime.now().toDate(), DateTime.now().plusDays(10).toDate()));
		assertTrue(notification.isBetween(DateTime.now().plusDays(30).toDate(), DateTime.now().plusDays(40).toDate()));
	}
}
