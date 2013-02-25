package org.netcook.android.security;

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
	public static final int PBE_ITERATION_COUNT = 200; // 1024;

	private static final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

	final byte[] nInitializationVector;
	final byte[] nEncryptionSalt;
	final String nPassword;

	public Crypter(String password, String encryptionSalt, String initializationVector) {
		nEncryptionSalt = encryptionSalt.getBytes();
		nInitializationVector = initializationVector.getBytes();
		nPassword = password;
	}

	public byte[] encrypt(String cleartext) {
		byte[] encryptedText = null;

		try {

			PBEKeySpec pbeKeySpec = new PBEKeySpec(nPassword.toCharArray(), nEncryptionSalt, PBE_ITERATION_COUNT, 256);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);

			IvParameterSpec ivspec = new IvParameterSpec(nInitializationVector);

			encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
			encryptedText = encryptionCipher.doFinal(cleartext.getBytes());

		} catch (Exception e) {
			Log.d(TAG, "encrypt failed", e);
		}

		return encryptedText;
	}

	public String decrypt(String encryptedText) {
		String cleartext = "";
		try {
			PBEKeySpec pbeKeySpec = new PBEKeySpec(nPassword.toCharArray(), nEncryptionSalt, PBE_ITERATION_COUNT, 256);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
			SecretKey tmp = factory.generateSecret(pbeKeySpec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
			IvParameterSpec ivspec = new IvParameterSpec(nInitializationVector);

			decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
			byte[] decryptedText = decryptionCipher.doFinal(encryptedText.getBytes());
			cleartext = new String(decryptedText);

		} catch (Exception e) {
			Log.d(TAG, "decrypt failed", e);
		}
		return cleartext;
	}
}