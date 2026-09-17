package com.radiofind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidatePasswordResetTokenRequest {

  @NotBlank
  private String token;
}
