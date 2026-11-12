package com.radiofind.account.dto;

import com.radiofind.auth.entity.TwoFactorMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartTwoFactorEnrollmentRequest(@NotNull TwoFactorMethod method, @NotBlank String password) {
}
