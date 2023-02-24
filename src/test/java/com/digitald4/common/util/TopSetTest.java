package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;

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

		String prev = "0";
		for (String s : topSet) {
			assertThat(s).isGreaterThan(prev);
			prev = s;
		}
		assertThat(topSet.first()).isEqualTo("1");
		assertThat(topSet.last()).isEqualTo("C");
	}

	@Test
	public void testRevOrdered() {
		TopSet<String> topSet = new TopSet<>(6, Comparator.<String>reverseOrder());
		topSet.addAll(SAMPLE_LIST);

		String prev = "zzz";
		for (String s : topSet) {
			assertThat(s).isLessThan(prev);
			prev = s;
		}
		assertThat(topSet.first()).isEqualTo("t");
		assertThat(topSet.last()).isEqualTo("D");
	}
}
