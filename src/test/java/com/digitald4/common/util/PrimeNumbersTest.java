package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.Test;

public class PrimeNumbersTest {
	@Test
	public void testIsPrime() {
		assertThat(PrimeNumbers.isPrime(1)).isTrue();
		assertThat(PrimeNumbers.isPrime(2)).isTrue();
		assertThat(PrimeNumbers.isPrime(17)).isTrue();

		assertThat(PrimeNumbers.isPrime(16)).isFalse();
		assertThat(PrimeNumbers.isPrime(25)).isFalse();
		assertThat(PrimeNumbers.isPrime(51)).isFalse();
	}

	@Test
	public void testGetPrimes() {
		List<Long> primes = PrimeNumbers.getPrimes(32);
		assertThat(primes.get(0)).isEqualTo(1L);
		assertThat(primes.get(1)).isEqualTo(2L);
		assertThat(primes.get(31)).isEqualTo(127L);
	}
}
