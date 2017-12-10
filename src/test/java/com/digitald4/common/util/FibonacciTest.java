package com.digitald4.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FibonacciTest {
	@Test
	public void testGenSequence() {
		int[] fibonacci = Fibonacci.genSequence(10);
		assertEquals(1, fibonacci[0]);
		assertEquals(1, fibonacci[1]);
		assertEquals(2, fibonacci[2]);
		assertEquals(3, fibonacci[3]);
		assertEquals(5, fibonacci[4]);
		assertEquals(34, fibonacci[8]);
	}
}
