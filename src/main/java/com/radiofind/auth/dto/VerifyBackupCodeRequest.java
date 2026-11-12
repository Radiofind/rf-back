package com.radiofind.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyBackupCodeRequest(@NotBlank String challengeId, @NotBlank @Size(max = 20) String code) {
}
