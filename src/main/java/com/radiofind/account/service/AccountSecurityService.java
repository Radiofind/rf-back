package com.radiofind.account.service;

import com.radiofind.account.dto.BackupCodesResponse;
import com.radiofind.account.dto.SecurityOverviewResponse;
import com.radiofind.account.dto.StartTwoFactorEnrollmentRequest;
import com.radiofind.account.dto.TwoFactorEnrollmentResponse;
import com.radiofind.auth.entity.TwoFactorEnrollment;
import com.radiofind.auth.entity.TwoFactorMethod;
import com.radiofind.auth.repository.TwoFactorEnrollmentRepository;
import com.radiofind.auth.service.BackupCodeService;
import com.radiofind.auth.service.Totp;
import com.radiofind.auth.service.TotpSecretCipher;
import com.radiofind.auth.service.TwoFactorDelivery;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;

@Service
@RequiredArgsConstructor
public class AccountSecurityService {

  private static final Duration ENROLLMENT_LIFETIME = Duration.ofMinutes(10);
  private static final int MAX_ENROLLMENT_ATTEMPTS = 5;
  private static final int CODE_BOUND = 1_000_000;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final BackupCodeService backupCodeService;
  private final PasswordConfirmation passwordConfirmation;
  private final TwoFactorEnrollmentRepository enrollmentRepository;
  private final TwoFactorDelivery twoFactorDelivery;
  private final Totp totp;
  private final TotpSecretCipher totpSecretCipher;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Transactional(readOnly = true)
  public SecurityOverviewResponse getOverview(CustomUserDetails userDetails) {
    return overviewOf(userDetails.getUser());
  }

  @Transactional
  public TwoFactorEnrollmentResponse startEnrollment(
      CustomUserDetails userDetails,
      StartTwoFactorEnrollmentRequest request) {
    User user = findPersistedUser(userDetails);
    TwoFactorMethod method = request.method();

    passwordConfirmation.require(user, request.password());
    requireUsable(user, method);

    TwoFactorEnrollment enrollment = enrollmentRepository.findByUserId(user.getId())
        .orElseGet(() -> TwoFactorEnrollment.builder().user(user).build());
    enrollment.setMethod(method);
    enrollment.setAttempts(0);
    enrollment.setExpiresAt(Instant.now(clock).plus(ENROLLMENT_LIFETIME));

    if (method == TwoFactorMethod.AUTHENTICATOR) {
      String secret = totp.generateSecret();
      enrollment.setTotpSecret(totpSecretCipher.encrypt(secret, user.getId()));
      enrollment.setCodeHash(null);
      enrollmentRepository.save(enrollment);

      return new TwoFactorEnrollmentResponse(method, null, secret, totp.uri(secret, user.getEmail()));
    }

    String code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(CODE_BOUND));
    enrollment.setTotpSecret(null);
    enrollment.setCodeHash(passwordEncoder.encode(code));
    enrollmentRepository.save(enrollment);
    twoFactorDelivery.send(user, method, code);

    return new TwoFactorEnrollmentResponse(method, twoFactorDelivery.maskedDestination(user, method), null, null);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public SecurityOverviewResponse confirmEnrollment(CustomUserDetails userDetails, String code) {
    User user = findPersistedUser(userDetails);
    TwoFactorEnrollment enrollment = enrollmentRepository.findByUserId(user.getId())
        .filter(found -> found.getExpiresAt().isAfter(Instant.now(clock)))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Two-factor setup has expired"));

    if (enrollment.getAttempts() >= MAX_ENROLLMENT_ATTEMPTS) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts");
    }

    enrollment.setAttempts(enrollment.getAttempts() + 1);

    OptionalLong step = matchingStep(user, enrollment, code);

    if (step.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Invalid verification code");
    }

    user.setTwoFactorMethod(enrollment.getMethod());
    user.setTotpSecret(enrollment.getTotpSecret());
    user.setTotpLastUsedStep(enrollment.getTotpSecret() == null ? null : step.getAsLong());
    enrollmentRepository.delete(enrollment);

    return overviewOf(user);
  }

  @Transactional
  public BackupCodesResponse regenerateBackupCodes(CustomUserDetails userDetails, String password) {
    User user = findPersistedUser(userDetails);

    passwordConfirmation.require(user, password);

    return new BackupCodesResponse(backupCodeService.regenerate(user));
  }

  private void requireUsable(User user, TwoFactorMethod method) {
    if (method == TwoFactorMethod.RECOVERY_EMAIL && user.getRecoveryEmail() == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No recovery email is set");
    }
  }

  private OptionalLong matchingStep(User user, TwoFactorEnrollment enrollment, String code) {
    if (enrollment.getMethod() == TwoFactorMethod.AUTHENTICATOR) {
      return totp.matchingStep(totpSecretCipher.decrypt(enrollment.getTotpSecret(), user.getId()), code);
    }

    return passwordEncoder.matches(code, enrollment.getCodeHash()) ? OptionalLong.of(0) : OptionalLong.empty();
  }

  private SecurityOverviewResponse overviewOf(User user) {
    return new SecurityOverviewResponse(
        user.getTwoFactorMethod(),
        twoFactorDelivery.mask(user.getEmail()),
        twoFactorDelivery.mask(user.getRecoveryEmail()),
        backupCodeService.countRemaining(user.getId()));
  }

  private User findPersistedUser(CustomUserDetails userDetails) {
    return userRepository.findById(userDetails.getUser().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }
}
