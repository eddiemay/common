package com.digitald4.common.util;

import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Test;

public class CalculateTest {
	
	@Test
	public void testGetWeekRange() {
		Pair<DateTime, DateTime> range = Calculate.getWeekRange(DateTime.parse("2014-10-01"));
		DateTime start = range.getLeft();
		DateTime end = range.getRight();
		assertEquals(2014, start.getYear());
		assertEquals(9, start.getMonthOfYear());
		assertEquals(28, start.getDayOfMonth());
		assertEquals(0, start.getMillisOfDay());
		assertEquals(2014, end.getYear());
		assertEquals(10, end.getMonthOfYear());
		assertEquals(4, end.getDayOfMonth());
		assertEquals(Calculate.LAST_MILLI_OF_DAY, end.getMillisOfDay());
	}
	
	@Test
	public void testGetMonthRange() {
		Pair<DateTime, DateTime> range = Calculate.getMonthRange(2014, 10);
		DateTime start = range.getLeft();
		DateTime end = range.getRight();
		assertEquals(2014, start.getYear());
		assertEquals(10, start.getMonthOfYear());
		assertEquals(1, start.getDayOfMonth());
		assertEquals(0, start.getMillisOfDay());
		assertEquals(2014, end.getYear());
		assertEquals(10, end.getMonthOfYear());
		assertEquals(31, end.getDayOfMonth());
		assertEquals(Calculate.LAST_MILLI_OF_DAY, end.getMillisOfDay());
	}

	@Test
	public void testGetCalMonthRange() {
		Pair<DateTime, DateTime> range = Calculate.getCalMonthRange(2014, 10);
		DateTime start = range.getLeft();
		DateTime end = range.getRight();
		assertEquals(2014, start.getYear());
		assertEquals(9, start.getMonthOfYear());
		assertEquals(28, start.getDayOfMonth());
		assertEquals(0, start.getMillisOfDay());
		assertEquals(2014, end.getYear());
		assertEquals(11, end.getMonthOfYear());
		assertEquals(1, end.getDayOfMonth());
		assertEquals(Calculate.LAST_MILLI_OF_DAY, end.getMillisOfDay());
		
		range = Calculate.getCalMonthRange(2014, 11);
		start = range.getLeft();
		end = range.getRight();
		assertEquals(10, start.getMonthOfYear());
		assertEquals(26, start.getDayOfMonth());
		assertEquals(12, end.getMonthOfYear());
		assertEquals(6, end.getDayOfMonth());
		
		range = Calculate.getCalMonthRange(2014, 6);
		start = range.getLeft();
		end = range.getRight();
		assertEquals(6, start.getMonthOfYear());
		assertEquals(1, start.getDayOfMonth());
		assertEquals(7, end.getMonthOfYear());
		assertEquals(5, end.getDayOfMonth());
		
		range = Calculate.getCalMonthRange(2014, 5);
		start = range.getLeft();
		end = range.getRight();
		assertEquals(4, start.getMonthOfYear());
		assertEquals(27, start.getDayOfMonth());
		assertEquals(5, end.getMonthOfYear());
		assertEquals(31, end.getDayOfMonth());
	}
	
	@Test
	public void testGetYearRange() {
		Pair<DateTime, DateTime> range = Calculate.getYearRange(2014);
		DateTime start = range.getLeft();
		DateTime end = range.getRight();
		assertEquals(2014, start.getYear());
		assertEquals(1, start.getMonthOfYear());
		assertEquals(1, start.getDayOfMonth());
		assertEquals(0, start.getMillisOfDay());
		assertEquals(2014, end.getYear());
		assertEquals(12, end.getMonthOfYear());
		assertEquals(31, end.getDayOfMonth());
		assertEquals(Calculate.LAST_MILLI_OF_DAY, end.getMillisOfDay());
	}

	@Test
	public void testCalcAverage() {
		assertEquals(394, Calculate.calcAverage(600, 470, 170, 430, 300), .001);
	}

	@Test
	public void testVariance() {
		assertEquals(27130, Calculate.variance(600, 470, 170, 430, 300), .001);
	}

	@Test
	public void testStandardDeviation() {
		assertEquals(164.712, Calculate.standardDeviation(600, 470, 170, 430, 300), .001);
	}

	@Test
	public void testFactorial() {
		assertEquals(2, Calculate.factorial(2));
		assertEquals(24, Calculate.factorial(4));
		assertEquals(362880, Calculate.factorial(9));
	}

	@Test
	public void testCombinations() {
		assertEquals(45, Calculate.combinations(10, 2));
	}
}
