package com.digitald4.common.util;

import com.digitald4.common.exception.DD4StorageException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class Encryptor {
	private static final String ALGORITHM = "AES";

	private final SecretKeySpec secretKey;

	public Encryptor(String key) {
		secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
	}

	/**
	 * Encrypts the given plain text
	 *
	 * @param plainText The plain text to encrypt
	 */
	public String encrypt(String plainText) {
		return Calculate.toHex(encrypt(plainText.getBytes()));
	}

	private byte[] encrypt(byte[] plainText) {
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return cipher.doFinal(plainText);
		} catch (Exception e) {
			throw new DD4StorageException("Unable to encrypt", e);
		}
	}

	/**
	 * Decrypts the given byte array
	 *
	 * @param cipherText The data to decrypt
	 */
	public String decrypt(String cipherText) throws Exception {
		return new String(decrypt(DatatypeConverter.parseHexBinary(cipherText)));
	}

	private byte[] decrypt(byte[] cipherText) throws Exception {
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return cipher.doFinal(cipherText);
	}
}