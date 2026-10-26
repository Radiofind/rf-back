package com.radiofind.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @NotBlank
  @Size(max = 100)
  private String surname;

  @NotBlank
  @Email
  @Size(max = 254)
  private String email;

  @Email
  @Size(max = 254)
  private String recoveryEmail;

  @NotNull
  @Past
  private LocalDate dateOfBirth;

  @NotBlank
  @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
  private String password;

  @Valid
  private ArtistInformationDto artistInformation;
}
