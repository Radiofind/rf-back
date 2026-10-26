package com.radiofind.user.dto;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpdateUserProfileRequestValidationTest {

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

  private UpdateUserProfileRequest validRequest() {
    UpdateUserProfileRequest request = new UpdateUserProfileRequest();
    request.setName("Ada");
    request.setSurname("Lovelace");
    request.setEmail("ada@example.com");
    return request;
  }

  @Test
  void validRequestHasNoViolations() {
    assertEquals(Set.of(), invalidFields(validRequest()));
  }

  @Test
  void requiresNameSurnameAndEmail() {
    assertEquals(Set.of("name", "surname", "email"), invalidFields(new UpdateUserProfileRequest()));
  }

  @Test
  void rejectsInvalidEmailsAndTooLongValues() {
    UpdateUserProfileRequest request = validRequest();
    request.setEmail("not-an-email");
    request.setRecoveryEmail("also-not-an-email");
    request.setArtistName("a".repeat(151));
    request.setShortDescription("a".repeat(501));
    request.setDescription("a".repeat(3001));

    assertEquals(
        Set.of("email", "recoveryEmail", "artistName", "shortDescription", "description"),
        invalidFields(request));
  }

  @Test
  void validatesGenresAndNestedSocialLinks() {
    UpdateUserProfileRequest request = validRequest();
    request.setMainGenres(List.of("Rock", "a".repeat(101)));
    request.setSocialLinks(ArtistSocialLinksDto.builder().instagram("https://x.com/" + "a".repeat(2048)).build());

    assertEquals(Set.of("mainGenres[1].<list element>", "socialLinks.instagram"), invalidFields(request));
  }
}
