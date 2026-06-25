package com.radiofind.auth.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

  private String name;

  private String surname;

  private String email;

  private String recoveryEmail;

  private LocalDate dateOfBirth;

  private String password;

  private ArtistInformationDto artistInformation;
}
