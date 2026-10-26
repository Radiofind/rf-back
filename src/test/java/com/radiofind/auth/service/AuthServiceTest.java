package com.radiofind.auth.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.entity.ArtistType;
import com.radiofind.artist.repository.ArtistProfileRepository;
import com.radiofind.auth.dto.ArtistInformationDto;
import com.radiofind.auth.dto.AuthResponse;
import com.radiofind.auth.dto.ForgotPasswordRequest;
import com.radiofind.auth.dto.LoginRequest;
import com.radiofind.auth.dto.RegisterRequest;
import com.radiofind.auth.dto.ResendTwoFactorRequest;
import com.radiofind.auth.dto.ResetPasswordRequest;
import com.radiofind.auth.dto.ValidatePasswordResetTokenRequest;
import com.radiofind.auth.dto.VerifyTwoFactorRequest;
import com.radiofind.auth.entity.PasswordResetToken;
import com.radiofind.auth.entity.TwoFactorChallenge;
import com.radiofind.auth.repository.PasswordResetTokenRepository;
import com.radiofind.auth.repository.TwoFactorChallengeRepository;
import com.radiofind.notification.service.EmailService;
import com.radiofind.security.JwtService;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

  private static final String CHALLENGE_ID = "challenge-1";

  private static final String CODE_HASH = "hashed-code";

  private static final String RESET_TOKEN = "raw-reset-token";

  private UserRepository userRepository;

  private ArtistProfileRepository artistProfileRepository;

  private TwoFactorChallengeRepository twoFactorChallengeRepository;

  private PasswordResetTokenRepository passwordResetTokenRepository;

  private PasswordEncoder passwordEncoder;

  private JwtService jwtService;

  private EmailService emailService;

  private AuthService authService;

  private final User user = User.builder().id(7L).email("ada@example.com").password("old-hash").build();

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    artistProfileRepository = mock(ArtistProfileRepository.class);
    twoFactorChallengeRepository = mock(TwoFactorChallengeRepository.class);
    passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    jwtService = mock(JwtService.class);
    emailService = mock(EmailService.class);
    authService = new AuthService(
        userRepository,
        artistProfileRepository,
        twoFactorChallengeRepository,
        passwordEncoder,
        jwtService,
        emailService,
        passwordResetTokenRepository);
    ReflectionTestUtils.setField(authService, "frontendUrl", "https://radiofind.test");

    when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));
  }

  private TwoFactorChallenge challenge(int attempts, LocalDateTime expiresAt, LocalDateTime lastSentAt) {
    TwoFactorChallenge challenge = TwoFactorChallenge.builder()
        .challengeId(CHALLENGE_ID)
        .user(user)
        .codeHash(CODE_HASH)
        .expiresAt(expiresAt)
        .lastSentAt(lastSentAt)
        .attempts(attempts)
        .build();

    when(twoFactorChallengeRepository.findWithLockByChallengeId(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

    return challenge;
  }

  private TwoFactorChallenge activeChallenge(int attempts) {
    return challenge(attempts, LocalDateTime.now().plusMinutes(4), LocalDateTime.now().minusMinutes(1));
  }

  private VerifyTwoFactorRequest verifyRequest(String code) {
    VerifyTwoFactorRequest request = new VerifyTwoFactorRequest();
    request.setChallengeId(CHALLENGE_ID);
    request.setCode(code);
    return request;
  }

  private ResendTwoFactorRequest resendRequest() {
    ResendTwoFactorRequest request = new ResendTwoFactorRequest();
    request.setChallengeId(CHALLENGE_ID);
    return request;
  }

  private PasswordResetToken resetToken(boolean used, LocalDateTime expiresAt) {
    PasswordResetToken resetToken = PasswordResetToken.builder()
        .tokenHash(sha256(RESET_TOKEN))
        .user(user)
        .used(used)
        .expiresAt(expiresAt)
        .build();

    when(passwordResetTokenRepository.findByTokenHash(sha256(RESET_TOKEN))).thenReturn(Optional.of(resetToken));

    return resetToken;
  }

  private ResetPasswordRequest resetRequest() {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(RESET_TOKEN);
    request.setNewPassword("new-password");
    return request;
  }

  private boolean isResetTokenValid() {
    ValidatePasswordResetTokenRequest request = new ValidatePasswordResetTokenRequest();
    request.setToken(RESET_TOKEN);
    return authService.validatePasswordResetToken(request).isValid();
  }

  private static void assertRejected(HttpStatus status, String reason, Executable executable) {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, executable);

    assertEquals(status, exception.getStatusCode());
    assertEquals(reason, exception.getReason());
  }

  private LoginRequest loginRequest(String email, String password) {
    LoginRequest request = new LoginRequest();
    request.setEmail(email);
    request.setPassword(password);
    return request;
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void verifyTwoFactorRejectsUsedCode() {
    activeChallenge(0).setUsed(true);

    assertRejected(HttpStatus.BAD_REQUEST, "Verification code has already been used",
        () -> authService.verifyTwoFactor(verifyRequest("123456")));
    verifyNoInteractions(jwtService);
  }

  @Test
  void verifyTwoFactorRejectsExpiredCode() {
    challenge(0, LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusMinutes(5));

    assertRejected(HttpStatus.BAD_REQUEST, "Verification code has expired",
        () -> authService.verifyTwoFactor(verifyRequest("123456")));
    verifyNoInteractions(jwtService);
  }

  @Test
  void verifyTwoFactorRejectsWhenMaxAttemptsReached() {
    TwoFactorChallenge challenge = activeChallenge(5);

    assertRejected(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts",
        () -> authService.verifyTwoFactor(verifyRequest("123456")));
    assertEquals(5, challenge.getAttempts());
    verify(passwordEncoder, never()).matches(any(), any());
    verifyNoInteractions(jwtService);
  }

  @Test
  void verifyTwoFactorIncrementsAttemptsOnWrongCode() {
    TwoFactorChallenge challenge = activeChallenge(2);
    when(passwordEncoder.matches("000000", CODE_HASH)).thenReturn(false);

    assertRejected(HttpStatus.UNAUTHORIZED, "Invalid verification code",
        () -> authService.verifyTwoFactor(verifyRequest("000000")));
    assertEquals(3, challenge.getAttempts());
    assertFalse(challenge.isUsed());
    verify(twoFactorChallengeRepository).save(challenge);
    verifyNoInteractions(jwtService);
  }

  @Test
  void verifyTwoFactorReturnsJwtForValidCode() {
    TwoFactorChallenge challenge = activeChallenge(1);
    when(passwordEncoder.matches("123456", CODE_HASH)).thenReturn(true);
    when(jwtService.generateToken(user)).thenReturn("jwt-token");

    AuthResponse response = authService.verifyTwoFactor(verifyRequest("123456"));

    assertEquals("jwt-token", response.getToken());
    assertFalse(response.isRequiresTwoFactor());
    assertTrue(challenge.isUsed());
    verify(twoFactorChallengeRepository).save(challenge);
  }

  @Test
  void resendTwoFactorEnforcesCooldown() {
    TwoFactorChallenge challenge = challenge(3, LocalDateTime.now().plusMinutes(4), LocalDateTime.now().minusSeconds(10));

    assertRejected(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting a new code",
        () -> authService.resendTwoFactor(resendRequest()));
    assertEquals(3, challenge.getAttempts());
    assertEquals(CODE_HASH, challenge.getCodeHash());
    verifyNoInteractions(emailService);
  }

  @Test
  void resendTwoFactorResetsAttemptsAndSendsNewCode() {
    LocalDateTime lastSentAt = LocalDateTime.now().minusMinutes(1);
    TwoFactorChallenge challenge = challenge(4, LocalDateTime.now().plusMinutes(4), lastSentAt);

    AuthResponse response = authService.resendTwoFactor(resendRequest());

    ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendTwoFactorCode(eq("ada@example.com"), code.capture());
    assertTrue(code.getValue().matches("\\d{6}"));
    assertEquals("encoded:" + code.getValue(), challenge.getCodeHash());
    assertEquals(0, challenge.getAttempts());
    assertTrue(challenge.getLastSentAt().isAfter(lastSentAt));
    assertEquals(1, challenge.getResendCount());
    assertTrue(challenge.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(4)));
    verify(twoFactorChallengeRepository).save(challenge);
    assertTrue(response.isRequiresTwoFactor());
    assertEquals(CHALLENGE_ID, response.getChallengeId());
    assertNull(response.getToken());
  }

  @Test
  void resetPasswordRejectsUnknownToken() {
    assertRejected(HttpStatus.BAD_REQUEST, "Invalid password reset token",
        () -> authService.resetPassword(resetRequest()));
    assertFalse(isResetTokenValid());
    verify(userRepository, never()).save(any());
  }

  @Test
  void resetPasswordRejectsUsedToken() {
    resetToken(true, LocalDateTime.now().plusMinutes(10));

    assertRejected(HttpStatus.BAD_REQUEST, "Password reset token has already been used",
        () -> authService.resetPassword(resetRequest()));
    assertFalse(isResetTokenValid());
    assertEquals("old-hash", user.getPassword());
    verify(userRepository, never()).save(any());
  }

  @Test
  void resetPasswordRejectsExpiredToken() {
    resetToken(false, LocalDateTime.now().minusSeconds(1));

    assertRejected(HttpStatus.BAD_REQUEST, "Password reset token has expired",
        () -> authService.resetPassword(resetRequest()));
    assertFalse(isResetTokenValid());
    assertEquals("old-hash", user.getPassword());
    verify(userRepository, never()).save(any());
  }

  @Test
  void resetPasswordUpdatesPasswordAndMarksTokenUsed() {
    PasswordResetToken resetToken = resetToken(false, LocalDateTime.now().plusMinutes(10));

    assertTrue(isResetTokenValid());

    authService.resetPassword(resetRequest());

    assertEquals("encoded:new-password", user.getPassword());
    assertTrue(resetToken.isUsed());
    verify(userRepository).save(user);
    verify(passwordResetTokenRepository).save(resetToken);
  }

  @Test
  void forgotPasswordDoesNothingForUnknownEmail() {
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("ghost@example.com");

    authService.forgotPassword(request);

    verify(passwordResetTokenRepository, never()).save(any());
    verify(passwordResetTokenRepository, never()).deleteByUserAndUsedFalse(any());
    verifyNoInteractions(emailService);
  }

  @Test
  void forgotPasswordStoresTokenHashAndSendsLinkWithRawToken() {
    when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail(" Ada@Example.com ");

    authService.forgotPassword(request);

    ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendPasswordResetLink(eq("ada@example.com"), link.capture());
    String prefix = "https://radiofind.test/auth/reset-password?token=";
    assertTrue(link.getValue().startsWith(prefix));
    String rawToken = link.getValue().substring(prefix.length());

    ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(passwordResetTokenRepository).deleteByUserAndUsedFalse(user);
    verify(passwordResetTokenRepository).save(saved.capture());
    assertEquals(sha256(rawToken), saved.getValue().getTokenHash());
    assertFalse(saved.getValue().isUsed());
    assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
  }

  @Test
  void resendTwoFactorRejectsWhenResendLimitReached() {
    TwoFactorChallenge challenge = activeChallenge(5);
    challenge.setResendCount(3);

    assertRejected(HttpStatus.TOO_MANY_REQUESTS, "Too many code requests, please log in again",
        () -> authService.resendTwoFactor(resendRequest()));

    assertEquals(5, challenge.getAttempts());
    verifyNoInteractions(emailService);
  }

  @Test
  void verifyTwoFactorRejectsUnknownChallenge() {
    assertRejected(HttpStatus.BAD_REQUEST, "Invalid verification request",
        () -> authService.verifyTwoFactor(verifyRequest("123456")));
  }

  @Test
  void loginDoesNotRevealWhetherEmailIsRegistered() {
    when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

    assertRejected(HttpStatus.UNAUTHORIZED, "Invalid email or password",
        () -> authService.login(loginRequest("ghost@example.com", "password")));
    assertRejected(HttpStatus.UNAUTHORIZED, "Invalid email or password",
        () -> authService.login(loginRequest("ada@example.com", "wrong-password")));
    verifyNoInteractions(emailService);
  }

  @Test
  void loginFindsUserRegardlessOfEmailCase() {
    when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "old-hash")).thenReturn(true);

    AuthResponse response = authService.login(loginRequest("  ADA@Example.com ", "password"));

    assertTrue(response.isRequiresTwoFactor());
    ArgumentCaptor<TwoFactorChallenge> saved = ArgumentCaptor.forClass(TwoFactorChallenge.class);
    verify(twoFactorChallengeRepository).save(saved.capture());
    assertEquals(response.getChallengeId(), saved.getValue().getChallengeId());
    assertEquals(0, saved.getValue().getResendCount());
    verify(emailService).sendTwoFactorCode(eq("ada@example.com"), anyString());
  }

  @Test
  void registerStoresNormalizedEmail() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail(" Ada@Example.com ");
    request.setRecoveryEmail(" Backup@Example.com ");
    request.setPassword("password");

    authService.register(request);

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).existsByEmailIgnoreCase("ada@example.com");
    verify(userRepository, times(2)).save(saved.capture());
    assertEquals("ada@example.com", saved.getValue().getEmail());
    assertEquals("backup@example.com", saved.getValue().getRecoveryEmail());
  }

  @Test
  void registerRejectsEmailRegisteredInAnotherCase() {
    when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);
    RegisterRequest request = new RegisterRequest();
    request.setEmail("ADA@example.com");

    assertRejected(HttpStatus.CONFLICT, "Email already exists", () -> authService.register(request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerRejectsMissingEmail() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("  ");

    assertRejected(HttpStatus.BAD_REQUEST, "Email is required", () -> authService.register(request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerCreatesArtistProfileAndReturnsJwt() {
    when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
    ArtistInformationDto artistInformation = new ArtistInformationDto();
    artistInformation.setTypeOfArtist(ArtistType.BAND);
    artistInformation.setArtistName("The Engines");
    artistInformation.setDescription("Analytical engine band");
    RegisterRequest request = new RegisterRequest();
    request.setEmail("ada@example.com");
    request.setPassword("password");
    request.setArtistInformation(artistInformation);

    AuthResponse response = authService.register(request);

    ArgumentCaptor<ArtistProfile> artistProfile = ArgumentCaptor.forClass(ArtistProfile.class);
    verify(artistProfileRepository).save(artistProfile.capture());
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(2)).save(saved.capture());
    User user = saved.getValue();
    assertEquals(ArtistType.BAND, artistProfile.getValue().getType());
    assertEquals("The Engines", artistProfile.getValue().getArtistName());
    assertSame(user, artistProfile.getValue().getUser());
    assertSame(artistProfile.getValue(), user.getArtistProfile());
    assertEquals("encoded:password", user.getPassword());
    assertEquals("jwt-token", response.getToken());
    assertFalse(response.isRequiresTwoFactor());
  }

  @Test
  void registerCreatesDefaultArtistProfileWithoutArtistInformation() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("ada@example.com");
    request.setPassword("password");

    authService.register(request);

    ArgumentCaptor<ArtistProfile> artistProfile = ArgumentCaptor.forClass(ArtistProfile.class);
    verify(artistProfileRepository).save(artistProfile.capture());
    assertEquals(ArtistType.ARTIST, artistProfile.getValue().getType());
    assertNull(artistProfile.getValue().getArtistName());
    assertSame(artistProfile.getValue(), artistProfile.getValue().getUser().getArtistProfile());
  }

  @Test
  void registerDefaultsArtistTypeWhenItIsMissing() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("ada@example.com");
    request.setPassword("password");
    ArtistInformationDto artistInformation = new ArtistInformationDto();
    artistInformation.setArtistName("Ada");
    request.setArtistInformation(artistInformation);

    authService.register(request);

    ArgumentCaptor<ArtistProfile> artistProfile = ArgumentCaptor.forClass(ArtistProfile.class);
    verify(artistProfileRepository).save(artistProfile.capture());
    assertEquals(ArtistType.ARTIST, artistProfile.getValue().getType());
    assertEquals("Ada", artistProfile.getValue().getArtistName());
  }
}
