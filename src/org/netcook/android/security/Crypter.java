package org.netcook.android.security;

import java.math.BigInteger;
import java.security.AlgorithmParameters;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class Crypter {

	private static final String TAG = "Crypter";
	
	public static final int SALT_LENGTH = 20;
	public static final int PBE_ITERATION_COUNT = 1024;

//	private static final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
	private static final String PBE_ALGORITHM = "PBKDF2WithHmacSHA1";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

	final byte[] nEncryptionSalt;
	final String nPassword;

	public Crypter(String password, String encryptionSalt) {
		nEncryptionSalt = encryptionSalt.getBytes();
		nPassword = password;
	}
	public class EncryptResponse {
		public String encrypted;
		public String iv;
		public EncryptResponse(String encrypted, String iv) {
			this.encrypted = encrypted;
			this.iv = iv;
		}
		
	}
	public EncryptResponse encrypt(String cleartext) {
		byte[] encryptedText = null;
		byte[] iv = null;

		try {

			PBEKeySpec pbeKeySpec = new PBEKeySpec(nPassword.toCharArray(), nEncryptionSalt, PBE_ITERATION_COUNT, 256);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);

			encryptionCipher.init(Cipher.ENCRYPT_MODE, secret);
			AlgorithmParameters params = encryptionCipher.getParameters();
			iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			encryptedText = encryptionCipher.doFinal(cleartext.getBytes("UTF-8"));

		} catch (Exception e) {
			Log.d(TAG, "encrypt failed", e);
		}

		return new EncryptResponse(printHex(encryptedText), printHex(iv));
	}

	public String decrypt(String encryptedText, byte[] initializationVector) {
		String cleartext = "";
		try {
			PBEKeySpec pbeKeySpec = new PBEKeySpec(nPassword.toCharArray(), nEncryptionSalt, PBE_ITERATION_COUNT, 256);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			IvParameterSpec ivspec = new IvParameterSpec(initializationVector);

			decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
			byte[] decryptedText = decryptionCipher.doFinal(new BigInteger(encryptedText, 16).toByteArray());
			cleartext = new String(decryptedText);

		} catch (Exception e) {
			Log.d(TAG, "decrypt failed", e);
		}
		return cleartext;
	}

	private String printHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", (b & 0xFF)));
		}
		return sb.toString();
	}
}