package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RetryableTest {

	@Test
	public void test() {
		StringBuilder data = new StringBuilder();
		String out = Calculate.executeWithRetries(2, () -> {
			if (data.isEmpty()) {
				data.append("done");
				throw new IllegalArgumentException("Retry test planned error");
			}

			return data.toString();
		});

		assertThat(out).isEqualTo("done");
	}
}
