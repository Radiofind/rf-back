package com.radiofind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

  @NotBlank
  private String token;

  @NotBlank
  @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
  private String newPassword;

}
