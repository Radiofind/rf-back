package com.radiofind.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TotpSecretCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final String VERSION_PREFIX = "v1:";
  private static final int KEY_BYTES = 32;
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final SecretKeySpec key;

  public TotpSecretCipher(@Value("${app.security.totp-encryption-key}") String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());

    if (keyBytes.length != KEY_BYTES) {
      throw new IllegalStateException(
          "app.security.totp-encryption-key must be a Base64-encoded 32-byte key, generate one with: openssl rand -base64 32");
    }

    this.key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
  }

  public String encrypt(String secret, long userId) {
    byte[] iv = new byte[IV_BYTES];
    SECURE_RANDOM.nextBytes(iv);
    byte[] ciphertext = run(Cipher.ENCRYPT_MODE, iv, userId, secret.getBytes(StandardCharsets.UTF_8));

    return VERSION_PREFIX + Base64.getEncoder().encodeToString(
        ByteBuffer.allocate(IV_BYTES + ciphertext.length).put(iv).put(ciphertext).array());
  }

  public String decrypt(String encrypted, long userId) {
    if (encrypted == null || !encrypted.startsWith(VERSION_PREFIX)) {
      throw new IllegalStateException("Authenticator secret is not encrypted with a known format");
    }

    ByteBuffer payload = ByteBuffer.wrap(Base64.getDecoder().decode(encrypted.substring(VERSION_PREFIX.length())));
    byte[] iv = new byte[IV_BYTES];
    payload.get(iv);
    byte[] ciphertext = new byte[payload.remaining()];
    payload.get(ciphertext);

    return new String(run(Cipher.DECRYPT_MODE, iv, userId, ciphertext), StandardCharsets.UTF_8);
  }

  private byte[] run(int mode, byte[] iv, long userId, byte[] input) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
      cipher.updateAAD(("user:" + userId).getBytes(StandardCharsets.UTF_8));
      return cipher.doFinal(input);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Authenticator secret could not be processed", e);
    }
  }
}
