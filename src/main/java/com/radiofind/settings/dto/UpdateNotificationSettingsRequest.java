package com.radiofind.settings.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequest(
    @NotNull Boolean trackUpdates,
    @NotNull Boolean radioUpdates,
    @NotNull Boolean likes) {
}
