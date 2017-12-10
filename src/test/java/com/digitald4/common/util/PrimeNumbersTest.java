package com.digitald4.common.util;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class PrimeNumbersTest {
	@Test
	public void testIsPrime() {
		assertTrue(PrimeNumbers.isPrime(1));
		assertTrue(PrimeNumbers.isPrime(2));
		assertTrue(PrimeNumbers.isPrime(17));

		assertFalse(PrimeNumbers.isPrime(16));
		assertFalse(PrimeNumbers.isPrime(25));
		assertFalse(PrimeNumbers.isPrime(51));
	}

	@Test
	public void testGetPrimes() {
		List<Long> primes = PrimeNumbers.getPrimes(32);
		assertEquals(1L, primes.get(0).longValue());
		assertEquals(2L, primes.get(1).longValue());
		assertEquals(127L, primes.get(31).longValue());
	}
}
