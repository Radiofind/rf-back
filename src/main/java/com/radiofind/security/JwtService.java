package com.radiofind.security;

import com.radiofind.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.security.Key;

@Service
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private long expiration;

  private Key key = Keys.hmacShaKeyFor(secret.getBytes());

  public String generateToken(User user) {
    return Jwts.builder()
        .setSubject(user.getEmail())
        .claim("role", user.getRole().name())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }
}
