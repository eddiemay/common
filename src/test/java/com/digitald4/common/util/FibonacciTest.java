package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FibonacciTest {
	@Test
	public void testGenSequence() {
		int[] fibonacci = Fibonacci.genSequence(10);
		assertThat(fibonacci[0]).isEqualTo(1);
		assertThat(fibonacci[1]).isEqualTo(1);
		assertThat(fibonacci[2]).isEqualTo(2);
		assertThat(fibonacci[3]).isEqualTo(3);
		assertThat(fibonacci[4]).isEqualTo(5);
		assertThat(fibonacci[8]).isEqualTo(34);
	}
}
