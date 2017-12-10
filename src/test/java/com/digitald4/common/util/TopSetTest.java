package com.digitald4.common.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

public class TopSetTest {
	private static final List<String> SAMPLE_LIST =
			Arrays.asList("C", "P", "B", "2", "E", "D", "d", "1", "t", "e", "1", "A", "3");

	@Test
	public void testOrdered() {
		TopSet<String> topSet = new TopSet<>(6, Comparator.<String>naturalOrder());
		topSet.addAll(SAMPLE_LIST);

		String prev = topSet.first();
		for (String s : topSet) {
			assertTrue(prev.compareTo(s) < 1);
		}
		assertEquals("1", topSet.first());
		assertEquals("C", topSet.last());
	}

	@Test
	public void testRevOrdered() {
		TopSet<String> topSet = new TopSet<>(6, Comparator.<String>reverseOrder());
		topSet.addAll(SAMPLE_LIST);

		String prev = topSet.first();
		for (String s : topSet) {
			assertTrue(prev.compareTo(s) >= 0);
		}
		assertEquals("t", topSet.first());
		assertEquals("D", topSet.last());
	}
}
