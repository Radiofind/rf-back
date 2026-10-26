package com.radiofind.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRequestValidationTest {

  private static ValidatorFactory validatorFactory;

  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  private static Set<String> invalidFields(Object request) {
    return validator.validate(request).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  private RegisterRequest validRegisterRequest() {
    RegisterRequest request = new RegisterRequest();
    request.setName("Ada");
    request.setSurname("Lovelace");
    request.setEmail("ada@example.com");
    request.setDateOfBirth(LocalDate.of(1990, 12, 10));
    request.setPassword("password");
    return request;
  }

  @Test
  void validRegisterRequestHasNoViolations() {
    assertEquals(Set.of(), invalidFields(validRegisterRequest()));
  }

  @Test
  void registerRequestRequiresMandatoryFields() {
    assertEquals(
        Set.of("name", "surname", "email", "dateOfBirth", "password"),
        invalidFields(new RegisterRequest()));
  }

  @Test
  void registerRequestRejectsInvalidValues() {
    RegisterRequest request = validRegisterRequest();
    request.setEmail("not-an-email");
    request.setRecoveryEmail("also-not-an-email");
    request.setDateOfBirth(LocalDate.now().plusDays(1));
    request.setPassword("short");
    ArtistInformationDto artistInformation = new ArtistInformationDto();
    artistInformation.setArtistName("a".repeat(151));
    request.setArtistInformation(artistInformation);

    assertEquals(
        Set.of("email", "recoveryEmail", "dateOfBirth", "password", "artistInformation.artistName"),
        invalidFields(request));
  }

  @Test
  void passwordLongerThanBcryptLimitIsRejected() {
    RegisterRequest registerRequest = validRegisterRequest();
    registerRequest.setPassword("p".repeat(73));
    ResetPasswordRequest resetRequest = new ResetPasswordRequest();
    resetRequest.setToken("token");
    resetRequest.setNewPassword("p".repeat(73));

    assertEquals(Set.of("password"), invalidFields(registerRequest));
    assertEquals(Set.of("newPassword"), invalidFields(resetRequest));
  }

  @Test
  void loginRequestRequiresEmailAndPassword() {
    LoginRequest request = new LoginRequest();
    request.setEmail(" ");

    assertEquals(Set.of("email", "password"), invalidFields(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"12345", "1234567", "12a456", ""})
  void verifyTwoFactorRequestRequiresSixDigitCode(String code) {
    VerifyTwoFactorRequest request = new VerifyTwoFactorRequest();
    request.setChallengeId("challenge");
    request.setCode(code);

    assertTrue(invalidFields(request).contains("code"));
  }

  @Test
  void verifyTwoFactorRequestAcceptsSixDigitCode() {
    VerifyTwoFactorRequest request = new VerifyTwoFactorRequest();
    request.setChallengeId("challenge");
    request.setCode("012345");

    assertEquals(Set.of(), invalidFields(request));
  }
}
