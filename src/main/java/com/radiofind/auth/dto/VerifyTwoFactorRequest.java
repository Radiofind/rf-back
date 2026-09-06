package com.radiofind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyTwoFactorRequest {

  @NotBlank
  private String challengeId;

  @NotBlank
  @Pattern(regexp = "\\d{6}", message = "Code must contain exactly 6 digits")
  private String code;
}
