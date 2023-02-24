package com.digitald4.common.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class EncryptorTest {
	private static final String INPUT = "Test Input String.";

	private final Encryptor encryptor = new Encryptor("MySuperSecretKey");

	@Test
	public void test() throws Exception {
		String encrypted = encryptor.encrypt(INPUT);
		assertThat(encryptor.decrypt(encrypted)).isEqualTo(INPUT);
	}
}
