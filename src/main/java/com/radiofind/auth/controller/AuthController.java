package com.radiofind.auth.controller;

import com.radiofind.auth.dto.*;
import com.radiofind.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/verify-2fa")
  public AuthResponse verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request) {
    return authService.verifyTwoFactor(request);
  }
}
