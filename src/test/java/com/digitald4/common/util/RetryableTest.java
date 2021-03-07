package com.digitald4.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RetryableTest {

	@Test
	public void test() {
		StringBuilder data = new StringBuilder();
		String out = Calculate.executeWithRetries(2, () -> {
			if (data.length() == 0) {
				data.append("done");
				throw new IllegalArgumentException("Retry test planned error");
			}

			return data.toString();
		});

		assertEquals("done", out);
	}
}
