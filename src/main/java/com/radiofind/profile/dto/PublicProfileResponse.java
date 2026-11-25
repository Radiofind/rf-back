package com.radiofind.profile.dto;

import com.radiofind.profile.access.ProfileAccess;

import java.util.UUID;

public record PublicProfileResponse(
    UUID publicId,
    String displayName,
    String avatarUrl,
    ProfileAccess access,
    boolean own,
    boolean following,
    boolean canFollow,
    PublicProfileDetails details) {
}
