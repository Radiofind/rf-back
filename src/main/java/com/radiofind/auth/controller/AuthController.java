package com.radiofind.auth.controller;

import com.radiofind.auth.dto.*;
import com.radiofind.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

  @PostMapping("/resend-2fa")
  public AuthResponse resendTwoFactor(@Valid @RequestBody ResendTwoFactorRequest request) {
    return authService.resendTwoFactor(request);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok().build();
  }
}
