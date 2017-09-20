package com.digitald4.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EncryptorTest {
	private static final String input = "Test Input String.";

	private final Encryptor encryptor = new Encryptor("MySuperSecretKey");

	@Test
	public void test() throws Exception {
		String encrypted = encryptor.encrypt(input);
		assertEquals(input, encryptor.decrypt(encrypted));
	}
}
