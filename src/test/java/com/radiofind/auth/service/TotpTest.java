package com.radiofind.auth.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpTest {

  private static final String RFC_6238_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

  @Test
  void acceptsTheCodeOfTheRfcTestVector() {
    Totp totp = new Totp(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));

    OptionalLong step = totp.matchingStep(RFC_6238_SECRET, "287082");

    assertEquals(OptionalLong.of(1), step);
  }

  @Test
  void acceptsTheCodeOfThePreviousStep() {
    Totp totp = new Totp(Clock.fixed(Instant.ofEpochSecond(89), ZoneOffset.UTC));

    OptionalLong step = totp.matchingStep(RFC_6238_SECRET, "287082");

    assertEquals(OptionalLong.of(1), step);
  }

  @Test
  void rejectsACodeOlderThanTheAllowedDrift() {
    Totp totp = new Totp(Clock.fixed(Instant.ofEpochSecond(119), ZoneOffset.UTC));

    OptionalLong step = totp.matchingStep(RFC_6238_SECRET, "287082");

    assertTrue(step.isEmpty());
  }

  @Test
  void rejectsACodeThatIsNotSixDigits() {
    Totp totp = new Totp(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));

    OptionalLong step = totp.matchingStep(RFC_6238_SECRET, "28708");

    assertTrue(step.isEmpty());
  }

  @Test
  void otpauthUriNamesTheIssuerAndAccount() {
    Totp totp = new Totp(Clock.systemUTC());

    String uri = totp.uri("SECRET", "ada@example.com");

    assertTrue(uri.startsWith("otpauth://totp/RADIOFIND%3Aada%40example.com?secret=SECRET&issuer=RADIOFIND"));
  }
}
