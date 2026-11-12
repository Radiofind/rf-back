package com.radiofind.account.dto;

import com.radiofind.auth.entity.TwoFactorMethod;

public record SecurityOverviewResponse(
    TwoFactorMethod twoFactorMethod,
    String email,
    String recoveryEmail,
    long backupCodesRemaining) {
}
