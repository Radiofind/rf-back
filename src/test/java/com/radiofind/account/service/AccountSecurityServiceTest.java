package com.radiofind.account.service;

import com.radiofind.account.dto.StartTwoFactorEnrollmentRequest;
import com.radiofind.account.dto.TwoFactorEnrollmentResponse;
import com.radiofind.auth.entity.TwoFactorEnrollment;
import com.radiofind.auth.entity.TwoFactorMethod;
import com.radiofind.auth.repository.TwoFactorEnrollmentRepository;
import com.radiofind.auth.service.BackupCodeService;
import com.radiofind.auth.service.Totp;
import com.radiofind.auth.service.TotpSecretCipher;
import com.radiofind.auth.service.TwoFactorDelivery;
import com.radiofind.notification.service.EmailService;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

  @Mock
  private UserRepository userRepository;

  @Mock
  private BackupCodeService backupCodeService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TwoFactorEnrollmentRepository enrollmentRepository;

  @Mock
  private EmailService emailService;

  @Mock
  private Totp totp;

  @Mock
  private TotpSecretCipher totpSecretCipher;

  @Test
  void recoveryEmailSetupSendsTheCodeToTheRecoveryAddress() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).password("hash").email("ada@example.com")
        .recoveryEmail("backup@example.com").build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
    when(enrollmentRepository.findByUserId(7L)).thenReturn(Optional.empty());

    TwoFactorEnrollmentResponse response = service.startEnrollment(
        new CustomUserDetails(user), new StartTwoFactorEnrollmentRequest(TwoFactorMethod.RECOVERY_EMAIL, "secret"));

    verify(emailService).sendTwoFactorCode(eq("backup@example.com"), anyString());
    assertEquals("ba***@example.com", response.destination());
  }

  @Test
  void recoveryEmailCannotBeChosenWithoutOne() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).password("hash").email("ada@example.com").build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.startEnrollment(
        new CustomUserDetails(user), new StartTwoFactorEnrollmentRequest(TwoFactorMethod.RECOVERY_EMAIL, "secret")));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
  }

  @Test
  void methodStaysUnchangedWhenThePasswordIsWrong() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).password("hash").build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> service.startEnrollment(
        new CustomUserDetails(user), new StartTwoFactorEnrollmentRequest(TwoFactorMethod.AUTHENTICATOR, "wrong")));

    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  void confirmedAuthenticatorBecomesTheMethod() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).build();
    TwoFactorEnrollment enrollment = TwoFactorEnrollment.builder()
        .method(TwoFactorMethod.AUTHENTICATOR).totpSecret("v1:encrypted").expiresAt(NOW.plusSeconds(60)).build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(enrollmentRepository.findByUserId(7L)).thenReturn(Optional.of(enrollment));
    when(totpSecretCipher.decrypt("v1:encrypted", 7L)).thenReturn("SECRET");
    when(totp.matchingStep("SECRET", "123456")).thenReturn(OptionalLong.of(100L));

    service.confirmEnrollment(new CustomUserDetails(user), "123456");

    assertEquals(TwoFactorMethod.AUTHENTICATOR, user.getTwoFactorMethod());
    assertEquals("v1:encrypted", user.getTotpSecret());
    assertEquals(100L, user.getTotpLastUsedStep());
  }

  @Test
  void switchingBackToEmailForgetsTheAuthenticatorSecret() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).twoFactorMethod(TwoFactorMethod.AUTHENTICATOR)
        .totpSecret("v1:encrypted").totpLastUsedStep(100L).build();
    TwoFactorEnrollment enrollment = TwoFactorEnrollment.builder()
        .method(TwoFactorMethod.EMAIL).codeHash("hash").expiresAt(NOW.plusSeconds(60)).build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(enrollmentRepository.findByUserId(7L)).thenReturn(Optional.of(enrollment));
    when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

    service.confirmEnrollment(new CustomUserDetails(user), "123456");

    assertEquals(TwoFactorMethod.EMAIL, user.getTwoFactorMethod());
    assertNull(user.getTotpSecret());
    assertNull(user.getTotpLastUsedStep());
  }

  @Test
  void wrongCodeKeepsTheCurrentMethodAndUsesUpAnAttempt() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).build();
    TwoFactorEnrollment enrollment = TwoFactorEnrollment.builder()
        .method(TwoFactorMethod.AUTHENTICATOR).totpSecret("v1:encrypted").expiresAt(NOW.plusSeconds(60)).build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(enrollmentRepository.findByUserId(7L)).thenReturn(Optional.of(enrollment));
    when(totpSecretCipher.decrypt("v1:encrypted", 7L)).thenReturn("SECRET");
    when(totp.matchingStep("SECRET", "000000")).thenReturn(OptionalLong.empty());

    assertThrows(ResponseStatusException.class,
        () -> service.confirmEnrollment(new CustomUserDetails(user), "000000"));

    assertEquals(TwoFactorMethod.EMAIL, user.getTwoFactorMethod());
    assertEquals(1, enrollment.getAttempts());
  }

  @Test
  void expiredSetupCannotBeConfirmed() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).build();
    TwoFactorEnrollment enrollment = TwoFactorEnrollment.builder()
        .method(TwoFactorMethod.AUTHENTICATOR).totpSecret("SECRET").expiresAt(NOW.minusSeconds(1)).build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(enrollmentRepository.findByUserId(7L)).thenReturn(Optional.of(enrollment));

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.confirmEnrollment(new CustomUserDetails(user), "123456"));

    assertEquals(HttpStatus.GONE, error.getStatusCode());
  }

  @Test
  void authenticatorSecretIsStoredEncrypted() {
    AccountSecurityService service = new AccountSecurityService(userRepository, backupCodeService,
        new PasswordConfirmation(passwordEncoder), enrollmentRepository, new TwoFactorDelivery(emailService),
        totp, totpSecretCipher, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    User user = User.builder().id(7L).password("hash").email("ada@example.com").build();
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
    when(enrollmentRepository.findByUserId(7L)).thenReturn(Optional.empty());
    when(totp.generateSecret()).thenReturn("SECRET");
    when(totpSecretCipher.encrypt("SECRET", 7L)).thenReturn("v1:encrypted");
    ArgumentCaptor<TwoFactorEnrollment> saved = ArgumentCaptor.forClass(TwoFactorEnrollment.class);

    service.startEnrollment(
        new CustomUserDetails(user), new StartTwoFactorEnrollmentRequest(TwoFactorMethod.AUTHENTICATOR, "secret"));

    verify(enrollmentRepository).save(saved.capture());
    assertEquals("v1:encrypted", saved.getValue().getTotpSecret());
  }
}
