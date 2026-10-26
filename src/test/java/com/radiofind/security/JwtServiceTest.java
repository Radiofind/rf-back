package com.radiofind.security;

import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

  private static final String SECRET = "test-only-jwt-secret-key-at-least-32-bytes-long";

  private static final String FOREIGN_SECRET = "another-jwt-secret-key-that-is-at-least-32-bytes";

  private static final long ONE_HOUR = 3_600_000L;

  private final JwtService jwtService = new JwtService(SECRET, ONE_HOUR);

  private final User user = User.builder().email("ada@example.com").role(Role.USER).build();

  private CustomUserDetails userDetails(String email) {
    return new CustomUserDetails(User.builder().email(email).role(Role.USER).build());
  }

  @Test
  void generatedTokenContainsUsername() {
    String token = jwtService.generateToken(user);

    assertEquals("ada@example.com", jwtService.extractUsername(token));
    assertTrue(jwtService.isTokenValid(token, userDetails("ada@example.com")));
  }

  @Test
  void tokenIsNotValidForAnotherUser() {
    String token = jwtService.generateToken(user);

    assertFalse(jwtService.isTokenValid(token, userDetails("grace@example.com")));
  }

  @Test
  void expiredTokenIsRejected() {
    String token = new JwtService(SECRET, -1_000L).generateToken(user);

    assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(token));
    assertFalse(jwtService.isTokenValid(token, userDetails("ada@example.com")));
  }

  @Test
  void tokenWithForeignSignatureIsRejected() {
    String token = new JwtService(FOREIGN_SECRET, ONE_HOUR).generateToken(user);

    assertThrows(SignatureException.class, () -> jwtService.extractUsername(token));
    assertFalse(jwtService.isTokenValid(token, userDetails("ada@example.com")));
  }

  @Test
  void malformedTokenIsNotValid() {
    assertFalse(jwtService.isTokenValid("not-a-jwt", userDetails("ada@example.com")));
  }
}
