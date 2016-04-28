package com.digitald4.common.util;

import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Test;

public class ExpressionTest {

	@Test
	public void testEquals() {
		assertTrue(Expression.Equals.evaluate(5, 5));
		assertTrue(Expression.Equals.evaluate("Hello", "Hello"));
		assertTrue(Expression.Equals.evaluate("Hello", new String("Hello")));
		assertTrue(Expression.Equals.evaluate(DateTime.now().toDate(), DateTime.now().minusHours(2).toDate())); // Note this will fail in the early morning hours.
		assertTrue(Expression.Equals.evaluate(5.2341, 5.2341));
		assertFalse(Expression.Equals.evaluate(5, 6));
		assertFalse(Expression.Equals.evaluate("Hello", "Goodbye"));
		assertFalse(Expression.Equals.evaluate(5.2341, 5.2342));
		assertFalse(Expression.Equals.evaluate(DateTime.now().toDate(), DateTime.now().minusDays(2).toDate()));
		assertFalse(Expression.Equals.evaluate(DateTime.now().toDate(), null));
	}

	@Test
	public void testLessThan() {
		assertTrue(Expression.LessThan.evaluate(5, 6));
		assertTrue(Expression.LessThan.evaluate("Goodbye", "Hello"));
		assertTrue(Expression.LessThan.evaluate(5.234, 5.2341));
		assertTrue(Expression.LessThan.evaluate(DateTime.now().toDate(), DateTime.now().plusDays(2).toDate()));
		assertFalse(Expression.LessThan.evaluate(6, 5));
		assertFalse(Expression.LessThan.evaluate("Hello", "Goodbye"));
		assertFalse(Expression.LessThan.evaluate(5.2341, 5.234));
		assertFalse(Expression.LessThan.evaluate(DateTime.now().toDate(), DateTime.now().minusDays(2).toDate()));
		assertFalse(Expression.LessThan.evaluate(DateTime.now().toDate(), null));
	}

	@Test
	public void testLessThanOrEqualTo() {
		assertTrue(Expression.LessThanOrEqualTo.evaluate(5, 6));
		assertTrue(Expression.LessThanOrEqualTo.evaluate("Goodbye", "Hello"));
		assertTrue(Expression.LessThanOrEqualTo.evaluate(5.234, 5.2341));
		assertTrue(Expression.LessThanOrEqualTo.evaluate(5, 5));
		assertTrue(Expression.LessThanOrEqualTo.evaluate("Hello", "Hello"));
		assertTrue(Expression.LessThanOrEqualTo.evaluate(5.2341, 5.2341));
		assertFalse(Expression.LessThanOrEqualTo.evaluate(6, 5));
		assertFalse(Expression.LessThanOrEqualTo.evaluate("Hello", "Goodbye"));
		assertFalse(Expression.LessThanOrEqualTo.evaluate(5.2341, 5.234));
		assertFalse(Expression.LessThanOrEqualTo.evaluate(DateTime.now().toDate(), null));
	}

	@Test
	public void testGreaterThan() {
		assertTrue(Expression.GreaterThan.evaluate(6, 5));
		assertTrue(Expression.GreaterThan.evaluate("Hello", "Goodbye"));
		assertTrue(Expression.GreaterThan.evaluate(5.2341, 5.234));
		assertTrue(Expression.GreaterThan.evaluate(DateTime.now().toDate(), DateTime.now().minusDays(2).toDate()));
		assertFalse(Expression.GreaterThan.evaluate(5, 6));
		assertFalse(Expression.GreaterThan.evaluate("Goodbye", "Hello"));
		assertFalse(Expression.GreaterThan.evaluate(5.234, 5.2341));
		assertFalse(Expression.GreaterThan.evaluate(DateTime.now().toDate(), DateTime.now().plusDays(2).toDate()));
		assertFalse(Expression.GreaterThan.evaluate(DateTime.now().toDate(), null));
	}

	@Test
	public void testGreaterThanOrEqualTo() {
		assertTrue(Expression.GreaterThanOrEqualTo.evaluate(6, 5));
		assertTrue(Expression.GreaterThanOrEqualTo.evaluate("Hello", "Goodbye"));
		assertTrue(Expression.GreaterThanOrEqualTo.evaluate(5.2341, 5.234));
		assertTrue(Expression.GreaterThanOrEqualTo.evaluate(5, 5));
		assertTrue(Expression.GreaterThanOrEqualTo.evaluate("Hello", "Hello"));
		assertTrue(Expression.GreaterThanOrEqualTo.evaluate(5.2341, 5.2341));
		assertFalse(Expression.GreaterThanOrEqualTo.evaluate(5, 6));
		assertFalse(Expression.GreaterThanOrEqualTo.evaluate("Goodbye", "Hello"));
		assertFalse(Expression.GreaterThanOrEqualTo.evaluate(5.234, 5.2341));
		assertFalse(Expression.GreaterThanOrEqualTo.evaluate(DateTime.now().toDate(), null));
	}

	@Test
	public void testIsNull() {
		assertTrue(Expression.IsNull.evaluate(0));
		assertTrue(Expression.IsNull.evaluate(0, 5));
		assertTrue(Expression.IsNull.evaluate(null));
		assertTrue(Expression.IsNull.evaluate(null, null));
		assertTrue(Expression.IsNull.evaluate(""));
		assertTrue(Expression.IsNull.evaluate("", "yes"));
		assertTrue(Expression.IsNull.evaluate(0.0));
		assertFalse(Expression.IsNull.evaluate(5));
		assertFalse(Expression.IsNull.evaluate("Goodbye"));
		assertFalse(Expression.IsNull.evaluate(5.234));
	}

	@Test
	public void testNotNull() {
		assertTrue(Expression.NotNull.evaluate(5));
		assertTrue(Expression.NotNull.evaluate(5, null));
		assertTrue(Expression.NotNull.evaluate("Goodbye"));
		assertTrue(Expression.NotNull.evaluate("Goodbye", null));
		assertTrue(Expression.NotNull.evaluate(5.234));
		assertFalse(Expression.NotNull.evaluate(0));
		assertFalse(Expression.NotNull.evaluate(null));
		assertFalse(Expression.NotNull.evaluate(""));
		assertFalse(Expression.NotNull.evaluate(0.0));
	}
}
