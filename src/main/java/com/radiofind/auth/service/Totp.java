package com.radiofind.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;
import java.util.OptionalLong;

@Component
@RequiredArgsConstructor
public class Totp {

  private static final String ISSUER = "RADIOFIND";
  private static final String ALGORITHM = "HmacSHA1";
  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int SECRET_BYTES = 20;
  private static final int DIGITS = 6;
  private static final int MODULUS = 1_000_000;
  private static final long STEP_SECONDS = 30;
  private static final int ALLOWED_DRIFT_STEPS = 1;
  private static final long NO_MATCH = -1;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final Clock clock;

  public String generateSecret() {
    byte[] secret = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(secret);
    return encodeBase32(secret);
  }

  public String uri(String secret, String accountName) {
    String label = URLEncoder.encode(ISSUER + ":" + accountName, StandardCharsets.UTF_8).replace("+", "%20");

    return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
        .formatted(label, secret, ISSUER, DIGITS, STEP_SECONDS);
  }

  public OptionalLong matchingStep(String secret, String code) {
    if (secret == null || code == null || !code.matches("\\d{" + DIGITS + "}")) {
      return OptionalLong.empty();
    }

    byte[] key = decodeBase32(secret);
    byte[] expected = code.getBytes(StandardCharsets.US_ASCII);
    long currentStep = clock.instant().getEpochSecond() / STEP_SECONDS;
    long matched = NO_MATCH;

    for (long step = currentStep - ALLOWED_DRIFT_STEPS; step <= currentStep + ALLOWED_DRIFT_STEPS; step++) {
      if (MessageDigest.isEqual(codeAt(key, step).getBytes(StandardCharsets.US_ASCII), expected)) {
        matched = step;
      }
    }

    return matched == NO_MATCH ? OptionalLong.empty() : OptionalLong.of(matched);
  }

  private String codeAt(byte[] key, long step) {
    byte[] hash = hmac(key, ByteBuffer.allocate(Long.BYTES).putLong(step).array());
    int offset = hash[hash.length - 1] & 0x0f;
    int binary = ((hash[offset] & 0x7f) << 24)
        | ((hash[offset + 1] & 0xff) << 16)
        | ((hash[offset + 2] & 0xff) << 8)
        | (hash[offset + 3] & 0xff);

    return String.format(Locale.ROOT, "%0" + DIGITS + "d", binary % MODULUS);
  }

  private byte[] hmac(byte[] key, byte[] message) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return mac.doFinal(message);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA1 is not available", e);
    }
  }

  private String encodeBase32(byte[] data) {
    StringBuilder encoded = new StringBuilder();
    int buffer = 0;
    int bits = 0;

    for (byte value : data) {
      buffer = (buffer << 8) | (value & 0xff);
      bits += 8;

      while (bits >= 5) {
        encoded.append(BASE32_ALPHABET.charAt((buffer >> (bits - 5)) & 0x1f));
        bits -= 5;
      }
    }

    if (bits > 0) {
      encoded.append(BASE32_ALPHABET.charAt((buffer << (5 - bits)) & 0x1f));
    }

    return encoded.toString();
  }

  private byte[] decodeBase32(String encoded) {
    String normalized = encoded.replace("=", "").toUpperCase(Locale.ROOT);
    ByteBuffer decoded = ByteBuffer.allocate(normalized.length() * 5 / 8);
    int buffer = 0;
    int bits = 0;

    for (char character : normalized.toCharArray()) {
      buffer = (buffer << 5) | BASE32_ALPHABET.indexOf(character);
      bits += 5;

      if (bits >= 8) {
        decoded.put((byte) (buffer >> (bits - 8)));
        bits -= 8;
      }
    }

    return decoded.array();
  }
}
