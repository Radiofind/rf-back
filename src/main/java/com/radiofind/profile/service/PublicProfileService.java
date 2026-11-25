package com.radiofind.profile.service;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.entity.ArtistType;
import com.radiofind.profile.access.ProfileAccessResolver;
import com.radiofind.profile.access.ProfileViewing;
import com.radiofind.profile.dto.PublicProfileDetails;
import com.radiofind.profile.dto.PublicProfileResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.AvatarFiles;
import com.radiofind.storage.service.AvatarImage;
import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

  private static final String AVATAR_URL = "/api/users/%s/avatar";

  private final PublicProfiles publicProfiles;

  private final ProfileAccessResolver profileAccessResolver;

  private final ProfileCounters profileCounters;

  private final AvatarFiles avatarFiles;

  @Transactional(readOnly = true)
  public PublicProfileResponse getProfile(CustomUserDetails viewer, UUID publicId) {
    User owner = publicProfiles.require(publicId);
    ProfileViewing viewing = profileAccessResolver.resolve(viewer.getUser().getId(), owner);

    return new PublicProfileResponse(
        owner.getPublicId(),
        DisplayName.of(owner),
        viewing.showsAvatar() && avatarFiles.find(owner.getAvatarFileName()).isPresent()
            ? AVATAR_URL.formatted(owner.getPublicId())
            : null,
        viewing.access(),
        viewing.own(),
        viewing.following(),
        viewing.offersFollowing(),
        viewing.showsDetails() ? detailsOf(owner) : null);
  }

  @Transactional(readOnly = true)
  public Optional<AvatarImage> getAvatar(CustomUserDetails viewer, UUID publicId) {
    User owner = publicProfiles.require(publicId);

    if (!profileAccessResolver.resolve(viewer.getUser().getId(), owner).showsAvatar()) {
      return Optional.empty();
    }

    return avatarFiles.find(owner.getAvatarFileName());
  }

  private PublicProfileDetails detailsOf(User owner) {
    ArtistProfile artistProfile = owner.getArtistProfile() != null
        ? owner.getArtistProfile()
        : ArtistProfile.builder().build();

    return PublicProfileDetails.builder()
        .artistType(artistProfile.getType() != null ? artistProfile.getType() : ArtistType.DEFAULT)
        .mainGenres(List.copyOf(artistProfile.getMainGenres()))
        .additionalGenres(List.copyOf(artistProfile.getAdditionalGenres()))
        .shortDescription(artistProfile.getShortDescription())
        .description(artistProfile.getDescription())
        .socialLinks(ArtistSocialLinksDto.from(artistProfile.getSocialLinks()))
        .followersCount(profileCounters.followers(owner.getId()))
        .tracksCount(profileCounters.publishedTracks(owner.getId()))
        .likesCount(profileCounters.likesOfPublishedTracks(owner.getId()))
        .joinedAt(YearMonth.from(owner.getCreatedAt()))
        .build();
  }
}
