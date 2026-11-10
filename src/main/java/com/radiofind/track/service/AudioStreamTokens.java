package com.radiofind.track.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;

@Service
public class AudioStreamTokens {

  private static final Duration TTL = Duration.ofHours(6);

  private static final String SCOPE_CLAIM = "scope";

  private static final String AUDIO_SCOPE = "track-audio";

  private final Key key;

  private final Clock clock;

  public AudioStreamTokens(@Value("${jwt.secret}") String secret, Clock clock) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.clock = clock;
  }

  public String issue(long trackId) {
    return Jwts.builder()
        .setSubject(String.valueOf(trackId))
        .claim(SCOPE_CLAIM, AUDIO_SCOPE)
        .setExpiration(Date.from(clock.instant().plus(TTL)))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean grants(String token, long trackId) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(key)
          .setClock(() -> Date.from(clock.instant()))
          .build()
          .parseClaimsJws(token)
          .getBody();

      return AUDIO_SCOPE.equals(claims.get(SCOPE_CLAIM)) && String.valueOf(trackId).equals(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
