package com.radiofind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendTwoFactorRequest {

  @NotBlank
  private String challengeId;
}
