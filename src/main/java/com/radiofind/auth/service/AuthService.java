package com.radiofind.auth.service;

import com.radiofind.auth.dto.*;
import com.radiofind.auth.entity.*;
import com.radiofind.auth.repository.*;
import com.radiofind.notification.service.EmailService;
import com.radiofind.security.JwtService;
import com.radiofind.user.entity.*;
import com.radiofind.user.repository.UserRepository;
import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.entity.ArtistType;
import com.radiofind.artist.repository.ArtistProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
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
  private static final int MAX_RESENDS = 3;
  private static final int PASSWORD_RESET_EXPIRATION_MINUTES = 15;

  private final UserRepository userRepository;
  private final ArtistProfileRepository artistProfileRepository;
  private final TwoFactorChallengeRepository twoFactorChallengeRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Value("${app.frontend-url}")
  private String frontendUrl;

  public AuthResponse register(RegisterRequest request) {

    String email = normalizeEmail(request.getEmail());

    if (email == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
    }

    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    User user = User.builder()
        .name(request.getName())
        .surname(request.getSurname())
        .email(email)
        .recoveryEmail(normalizeEmail(request.getRecoveryEmail()))
        .dateOfBirth(request.getDateOfBirth())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .createdAt(LocalDateTime.now())
        .subscriptionType(SubscriptionType.FREE)
        .uploadsThisMonth(0)
        .build();

    userRepository.save(user);

    ArtistInformationDto artistInformation = Optional.ofNullable(request.getArtistInformation())
        .orElseGet(ArtistInformationDto::new);

    ArtistProfile artistProfile = ArtistProfile.builder()
        .type(Optional.ofNullable(artistInformation.getTypeOfArtist()).orElse(ArtistType.DEFAULT))
        .artistName(artistInformation.getArtistName())
        .description(artistInformation.getDescription())
        .user(user)
        .build();

    artistProfileRepository.save(artistProfile);

    user.setArtistProfile(artistProfile);

    userRepository.save(user);

    String token = jwtService.generateToken(user);

    return AuthResponse.builder()
        .token(token)
        .requiresTwoFactor(false)
        .build();
  }

  public AuthResponse login(LoginRequest request) {

    User user = findUserByEmail(request.getEmail())
        .filter(found -> request.getPassword() != null
            && passwordEncoder.matches(request.getPassword(), found.getPassword()))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password"));

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
        .resendCount(0)
        .build();

    twoFactorChallengeRepository.save(challenge);

    emailService.sendTwoFactorCode(user.getEmail(), code);

    return AuthResponse.builder()
        .requiresTwoFactor(true)
        .challengeId(challengeId)
        .build();
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request) {

    TwoFactorChallenge challenge = findChallengeForUpdate(request.getChallengeId());

    if (challenge.isUsed()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has already been used");
    }

    if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has expired");
    }

    if (challenge.getAttempts() >= MAX_ATTEMPTS) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts");
    }

    challenge.setAttempts(challenge.getAttempts() + 1);

    if (!passwordEncoder.matches(
        request.getCode(),
        challenge.getCodeHash())) {
      twoFactorChallengeRepository.save(challenge);

      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
    }

    challenge.setUsed(true);

    twoFactorChallengeRepository.save(challenge);

    String token = jwtService.generateToken(challenge.getUser());

    return AuthResponse.builder()
        .token(token)
        .requiresTwoFactor(false)
        .build();
  }

  @Transactional
  public AuthResponse resendTwoFactor(ResendTwoFactorRequest request) {

    TwoFactorChallenge challenge = findChallengeForUpdate(request.getChallengeId());

    if (challenge.isUsed()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has already been used");
    }

    LocalDateTime now = LocalDateTime.now();

    if (challenge.getExpiresAt().isBefore(now)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request has expired");
    }

    if (challenge.getResendCount() >= MAX_RESENDS) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many code requests, please log in again");
    }

    LocalDateTime nextAllowedSendTime = challenge
        .getLastSentAt()
        .plusSeconds(RESEND_COOLDOWN_SECONDS);

    if (now.isBefore(nextAllowedSendTime)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting a new code");
    }

    String code = generateCode();

    challenge.setCodeHash(passwordEncoder.encode(code));

    challenge.setAttempts(0);

    challenge.setResendCount(challenge.getResendCount() + 1);

    challenge.setLastSentAt(now);

    challenge.setExpiresAt(now.plusMinutes(CODE_EXPIRATION_MINUTES));

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

    findUserByEmail(request.getEmail()).ifPresent(user -> {

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
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token"));

    if (resetToken.isUsed()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token has already been used");
    }

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token has expired");
    }

    User user = resetToken.getUser();

    user.setPassword(
        passwordEncoder.encode(
            request.getNewPassword()));

    userRepository.save(user);

    resetToken.setUsed(true);

    passwordResetTokenRepository.save(resetToken);
  }

  public ValidatePasswordResetTokenResponse validatePasswordResetToken(ValidatePasswordResetTokenRequest request) {

    String tokenHash = hashToken(request.getToken());

    boolean valid = passwordResetTokenRepository
        .findByTokenHash(tokenHash)
        .map(resetToken -> !resetToken.isUsed() && resetToken.getExpiresAt().isAfter(LocalDateTime.now()))
        .orElse(false);

    return ValidatePasswordResetTokenResponse.builder()
        .valid(valid)
        .build();
  }

  private TwoFactorChallenge findChallengeForUpdate(String challengeId) {

    return twoFactorChallengeRepository
        .findWithLockByChallengeId(challengeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification request"));
  }

  private Optional<User> findUserByEmail(String email) {

    return Optional.ofNullable(normalizeEmail(email))
        .flatMap(userRepository::findByEmailIgnoreCase);
  }

  private String normalizeEmail(String email) {

    if (email == null || email.isBlank()) {
      return null;
    }

    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String generateCode() {

    int code = SECURE_RANDOM.nextInt(1_000_000);

    return String.format("%0" + CODE_LENGTH + "d", code);
  }

  private String generatePasswordResetToken() {

    byte[] randomBytes = new byte[32];

    SECURE_RANDOM.nextBytes(randomBytes);

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
