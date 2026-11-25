package com.radiofind.profile.dto;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import com.radiofind.artist.entity.ArtistType;
import lombok.Builder;

import java.time.YearMonth;
import java.util.List;

@Builder
public record PublicProfileDetails(
    ArtistType artistType,
    List<String> mainGenres,
    List<String> additionalGenres,
    String shortDescription,
    String description,
    ArtistSocialLinksDto socialLinks,
    long followersCount,
    long tracksCount,
    long likesCount,
    YearMonth joinedAt) {
}
