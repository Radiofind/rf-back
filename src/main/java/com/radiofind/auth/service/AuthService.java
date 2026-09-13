package com.radiofind.auth.service;

import com.radiofind.auth.dto.*;
import com.radiofind.auth.entity.*;
import com.radiofind.auth.repository.*;
import com.radiofind.notification.service.EmailService;
import com.radiofind.security.JwtService;
import com.radiofind.user.entity.*;
import com.radiofind.user.repository.UserRepository;
import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.repository.ArtistProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final int CODE_LENGTH = 6;
  private static final int CODE_EXPIRATION_MINUTES = 5;
  private static final int MAX_ATTEMPTS = 5;
  private static final int RESEND_COOLDOWN_SECONDS = 45;
  private static final int PASSWORD_RESET_EXPIRATION_MINUTES = 15;

  private final UserRepository userRepository;
  private final ArtistProfileRepository artistProfileRepository;
  private final TwoFactorChallengeRepository twoFactorChallengeRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;

  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${app.frontend-url}")
  private String frontendUrl;

  public AuthResponse register(RegisterRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    User user = User.builder()
        .name(request.getName())
        .surname(request.getSurname())
        .email(request.getEmail())
        .recoveryEmail(request.getRecoveryEmail())
        .dateOfBirth(request.getDateOfBirth())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .createdAt(LocalDateTime.now())
        .subscriptionType(SubscriptionType.FREE)
        .uploadsThisMonth(0)
        .build();

    userRepository.save(user);

    if (request.getArtistInformation() != null) {

      ArtistProfile artistProfile = ArtistProfile.builder()
          .type(request.getArtistInformation().getTypeOfArtist())
          .artistName(request.getArtistInformation().getArtistName())
          .description(request.getArtistInformation().getDescription())
          .user(user)
          .build();

      artistProfileRepository.save(artistProfile);

      user.setArtistProfile(artistProfile);

      userRepository.save(user);
    }

    String token = jwtService.generateToken(user);

    return AuthResponse.builder()
        .token(token)
        .requiresTwoFactor(false)
        .build();
  }

  public AuthResponse login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new RuntimeException("Invalid email or password");
    }

    String code = generateCode();

    String challengeId = UUID.randomUUID().toString();

    TwoFactorChallenge challenge = TwoFactorChallenge.builder()
        .challengeId(challengeId)
        .user(user)
        .codeHash(passwordEncoder.encode(code))
        .expiresAt(
            LocalDateTime.now()
                .plusMinutes(CODE_EXPIRATION_MINUTES))
        .lastSentAt(LocalDateTime.now())
        .used(false)
        .attempts(0)
        .build();

    twoFactorChallengeRepository.save(challenge);

    emailService.sendTwoFactorCode(user.getEmail(), code);

    return AuthResponse.builder()
        .requiresTwoFactor(true)
        .challengeId(challengeId)
        .build();
  }

  public AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request) {

    TwoFactorChallenge challenge = twoFactorChallengeRepository
        .findByChallengeId(request.getChallengeId())
        .orElseThrow(() -> new RuntimeException("Invalid verification request"));

    if (challenge.isUsed()) {
      throw new RuntimeException("Verification code has already been used");
    }

    if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Verification code has expired");
    }

    if (challenge.getAttempts() >= MAX_ATTEMPTS) {
      throw new RuntimeException("Too many attempts");
    }

    challenge.setAttempts(challenge.getAttempts() + 1);

    if (!passwordEncoder.matches(
        request.getCode(),
        challenge.getCodeHash())) {
      twoFactorChallengeRepository.save(challenge);

      throw new RuntimeException("Invalid verification code");
    }

    challenge.setUsed(true);

    twoFactorChallengeRepository.save(challenge);

    String token = jwtService.generateToken(challenge.getUser());

    return AuthResponse.builder()
        .token(token)
        .requiresTwoFactor(false)
        .build();
  }

  public AuthResponse resendTwoFactor(ResendTwoFactorRequest request) {

    TwoFactorChallenge challenge = twoFactorChallengeRepository
        .findByChallengeId(request.getChallengeId())
        .orElseThrow(() -> new RuntimeException("Invalid verification request"));

    if (challenge.isUsed()) {
      throw new RuntimeException("Verification code has already been used");
    }

    LocalDateTime now = LocalDateTime.now();

    if (challenge.getExpiresAt().isBefore(now)) {
      throw new RuntimeException("Verification request has expired");
    }

    LocalDateTime nextAllowedSendTime = challenge
        .getLastSentAt()
        .plusSeconds(RESEND_COOLDOWN_SECONDS);

    if (now.isBefore(nextAllowedSendTime)) {
      throw new RuntimeException("Please wait before requesting a new code");
    }

    String code = generateCode();

    challenge.setCodeHash(passwordEncoder.encode(code));

    challenge.setAttempts(0);

    challenge.setLastSentAt(now);

    twoFactorChallengeRepository.save(challenge);

    emailService.sendTwoFactorCode(challenge.getUser().getEmail(), code);

    return AuthResponse.builder()
        .token(null)
        .requiresTwoFactor(true)
        .challengeId(challenge.getChallengeId())
        .build();
  }

  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {

    userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

      passwordResetTokenRepository.deleteByUserAndUsedFalse(user);

      String rawToken = generatePasswordResetToken();

      String tokenHash = hashToken(rawToken);

      PasswordResetToken resetToken = PasswordResetToken.builder()
          .tokenHash(tokenHash)
          .user(user)
          .createdAt(LocalDateTime.now())
          .expiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRATION_MINUTES))
          .used(false)
          .build();

      passwordResetTokenRepository.save(resetToken);

      String resetLink = frontendUrl + "/auth/reset-password?token=" + rawToken;

      emailService.sendPasswordResetLink(user.getEmail(), resetLink);
    });
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {

    String tokenHash = hashToken(request.getToken());

    PasswordResetToken resetToken = passwordResetTokenRepository
        .findByTokenHash(tokenHash)
        .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

    if (resetToken.isUsed()) {
      throw new RuntimeException("Password reset token has already been used");
    }

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Password reset token has expired");
    }

    User user = resetToken.getUser();

    user.setPassword(
        passwordEncoder.encode(
            request.getNewPassword()));

    userRepository.save(user);

    resetToken.setUsed(true);

    passwordResetTokenRepository.save(resetToken);
  }

  private String generateCode() {

    int code = secureRandom.nextInt(1_000_000);

    return String.format("%0" + CODE_LENGTH + "d", code);
  }

  private String generatePasswordResetToken() {

    byte[] randomBytes = new byte[32];

    secureRandom.nextBytes(randomBytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String hashToken(String token) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();

      for (byte b : hash) {
        hexString.append(String.format("%02x", b));
      }

      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}
