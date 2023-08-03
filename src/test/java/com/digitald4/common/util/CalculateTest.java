package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.DELETE;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.EQUAL;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.INSERT;
import static org.junit.Assert.*;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.joda.time.DateTime;
import org.junit.Test;

public class CalculateTest {

	@Test
	public void testRound() {
		assertThat(Calculate.round(3.14159, 2)).isEqualTo(3.14);
		assertThat(Calculate.round(3.14159, 3)).isEqualTo(3.142);
		assertThat(Calculate.round(365.2422, 0)).isEqualTo(365);
	}

	@Test
	public void testRound_displayDecimal() {
		assertThat(Calculate.round(3.14159, true)).isEqualTo(3.1);
		assertThat(Calculate.round(3.14159, false)).isEqualTo(3);
		assertThat(Calculate.round(365.2422, false)).isEqualTo(365);
	}
	
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

	@Test
	public void testHexStringToByteArray() {
		assertThat(Calculate.hexStringToByteArray("BA05")).isEqualTo(new byte[]{(byte) 0xBA, 0x05});
		assertThat(Calculate.hexStringToByteArray("ba05")).isEqualTo(new byte[]{(byte) 0xBA, 0x05});
		assertThat(Calculate.hexStringToByteArray("2C3A6F2CF35EC2A0595BBE86C69CDA76")).isEqualTo(
				new byte[]{
						(byte) 0x2C, (byte) 0x3A, (byte) 0x6F, (byte) 0x2C, (byte) 0xF3, (byte) 0x5E, (byte) 0xC2, (byte) 0xA0,
						(byte) 0x59, (byte) 0x5B, (byte) 0xBE, (byte) 0x86, (byte) 0xC6, (byte) 0x9C, (byte) 0xDA, (byte) 0x76});
	}

	@Test
	public void testLD() {
		assertThat(Calculate.LD("ABCDELMN", "ABCFGLMN")).isEqualTo(2);
		assertThat(Calculate.LD("SATAN", "SANTA")).isEqualTo(2);
	}

	@Test
	public void testDiff() {
		assertThat(Calculate.getDiff("ABCDELMN", "ABCFGLMN")).containsExactly(new Diff(EQUAL, "ABC"),
				new Diff(DELETE, "DE"), new Diff(INSERT, "FG"), new Diff(EQUAL, "LMN"));
	}

	@Test
	public void testDiff_hebrew() {
		assertThat(Calculate.getDiff("כי ילד יולד לנו בן נתן לנו", "כי־ילד ילד־לנו בן נתן־לנו"))
				.containsExactly(new Diff(EQUAL, "כי"),
						new Diff(DELETE, " "), new Diff(INSERT, "־"), new Diff(EQUAL, "ילד י"),
						new Diff(DELETE, "ו"), new Diff(EQUAL,"לד"),
						new Diff(DELETE," "), new Diff(INSERT,"־"), new Diff(EQUAL,"לנו בן נתן"),
						new Diff(DELETE," "), new Diff(INSERT,"־"), new Diff(EQUAL,"לנו"));
	}

	@Test
	public void testDiffHtml() {
		assertThat(Calculate.getDiffHtml("ABCDELMN", "ABCFGLMN"))
				.isEqualTo("ABC<span class=\"diff-delete\">DE</span><span class=\"diff-insert\">FG</span>LMN");
	}

	@Test
	public void testDiffHtml_hebrew() {
		assertThat(Calculate.getDiffHtml("כי ילד יולד לנו בן נתן לנו", "כי־ילד ילד־לנו בן נתן־לנו"))
				.isEqualTo("כי<span class=\"diff-delete\"> </span><span class=\"diff-insert\">־</span>ילד י<span class=\"diff-delete\">ו</span>לד<span class=\"diff-delete\"> </span><span class=\"diff-insert\">־</span>לנו בן נתן<span class=\"diff-delete\"> </span><span class=\"diff-insert\">־</span>לנו");
	}
}
