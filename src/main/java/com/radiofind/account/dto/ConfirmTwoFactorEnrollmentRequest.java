package com.radiofind.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmTwoFactorEnrollmentRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
