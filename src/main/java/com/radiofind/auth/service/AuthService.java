package com.radiofind.auth.service;

import com.radiofind.auth.dto.*;
import com.radiofind.security.JwtService;
import com.radiofind.user.entity.*;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthResponse register(RegisterRequest request) {

    User user = User.builder()
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .subscriptionType(SubscriptionType.FREE)
        .uploadsThisMonth(0)
        .build();

    userRepository.save(user);

    String token = jwtService.generateToken(user);

    return new AuthResponse(token);
  }

  public AuthResponse login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new RuntimeException("Invalid password");
    }

    String token = jwtService.generateToken(user);

    return new AuthResponse(token);
  }
}
