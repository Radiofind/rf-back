package com.radiofind.account.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordConfirmationRequest(@NotBlank String password) {
}
