package com.radiofind.account.dto;

import com.radiofind.auth.entity.TwoFactorMethod;

public record TwoFactorEnrollmentResponse(
    TwoFactorMethod method,
    String destination,
    String secret,
    String otpauthUri) {
}
