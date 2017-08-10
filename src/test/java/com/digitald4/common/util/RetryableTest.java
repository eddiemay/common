package com.digitald4.common.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class RetryableTest {

	@Test
	public void test() throws Exception {
		String out = new Retryable<String, StringBuffer>() {
			@Override
			public String execute(StringBuffer data) throws Exception {
				if (data.length() == 0) {
					data.append("done");
					throw new IllegalArgumentException("Retry test planned error");
				}
				return data.toString();
			}
		}.run(new StringBuffer(""));
		assertEquals("done", out);
	}

}
