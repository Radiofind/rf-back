package com.radiofind.track.service;

import com.radiofind.security.JwtService;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioStreamTokensTest {

  private static final String SECRET = "test-only-jwt-secret-key-at-least-32-bytes-long";

  @Test
  void tokenGrantsTheAudioOfItsTrack() {
    AudioStreamTokens tokens = new AudioStreamTokens(SECRET, Clock.systemUTC());

    boolean granted = tokens.grants(tokens.issue(42L), 42L);

    assertTrue(granted);
  }

  @Test
  void tokenDoesNotGrantAnotherTrack() {
    AudioStreamTokens tokens = new AudioStreamTokens(SECRET, Clock.systemUTC());

    boolean granted = tokens.grants(tokens.issue(42L), 43L);

    assertFalse(granted);
  }

  @Test
  void tokenExpiresAfterSixHours() {
    Instant issuedAt = Instant.parse("2026-01-01T10:00:00Z");
    String token = new AudioStreamTokens(SECRET, Clock.fixed(issuedAt, ZoneOffset.UTC)).issue(42L);
    AudioStreamTokens later = new AudioStreamTokens(
        SECRET, Clock.fixed(Instant.parse("2026-01-01T16:00:01Z"), ZoneOffset.UTC));

    boolean granted = later.grants(token, 42L);

    assertFalse(granted);
  }

  @Test
  void loginTokenDoesNotGrantAudio() {
    String loginToken = new JwtService(SECRET, 3_600_000L)
        .generateToken(User.builder().email("42").role(Role.USER).build());

    boolean granted = new AudioStreamTokens(SECRET, Clock.systemUTC()).grants(loginToken, 42L);

    assertFalse(granted);
  }
}
