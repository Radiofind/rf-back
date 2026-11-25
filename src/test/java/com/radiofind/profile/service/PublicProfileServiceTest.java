package com.radiofind.profile.service;

import com.radiofind.profile.access.ProfileAccess;
import com.radiofind.profile.access.ProfileAccessResolver;
import com.radiofind.profile.access.ProfileViewing;
import com.radiofind.profile.dto.PublicProfileDetails;
import com.radiofind.profile.dto.PublicProfileResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.AvatarFiles;
import com.radiofind.storage.service.AvatarImage;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {

  private static final UUID PUBLIC_ID = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");

  @Mock
  private PublicProfiles publicProfiles;

  @Mock
  private ProfileAccessResolver profileAccessResolver;

  @Mock
  private ProfileCounters profileCounters;

  @Mock
  private AvatarFiles avatarFiles;

  @InjectMocks
  private PublicProfileService publicProfileService;

  @Test
  void hiddenProfileRevealsNeitherDetailsNorAvatar() {
    User owner = User.builder().id(1L).publicId(PUBLIC_ID).name("Ada").surname("Lovelace")
        .avatarFileName("avatar.png").build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(owner);
    when(profileAccessResolver.resolve(7L, owner))
        .thenReturn(new ProfileViewing(ProfileAccess.HIDDEN, false, false));

    PublicProfileResponse profile = publicProfileService.getProfile(viewer(), PUBLIC_ID);

    assertNull(profile.details());
    assertNull(profile.avatarUrl());
    verifyNoInteractions(avatarFiles, profileCounters);
  }

  @Test
  void restrictedProfileShowsTheAvatarButNoDetails() {
    User owner = User.builder().id(1L).publicId(PUBLIC_ID).name("Ada").surname("Lovelace")
        .avatarFileName("avatar.png").build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(owner);
    when(profileAccessResolver.resolve(7L, owner))
        .thenReturn(new ProfileViewing(ProfileAccess.RESTRICTED, false, false));
    when(avatarFiles.find("avatar.png"))
        .thenReturn(Optional.of(new AvatarImage(new ByteArrayResource(new byte[0]), MediaType.IMAGE_PNG)));

    PublicProfileResponse profile = publicProfileService.getProfile(viewer(), PUBLIC_ID);

    assertEquals("/api/users/6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f/avatar", profile.avatarUrl());
    assertNull(profile.details());
  }

  @Test
  void fullProfileShowsPublishedCountersAndOnlyTheMonthOfJoining() {
    User owner = User.builder().id(1L).publicId(PUBLIC_ID).name("Ada").surname("Lovelace")
        .createdAt(LocalDateTime.parse("2025-03-14T09:26:53")).build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(owner);
    when(profileAccessResolver.resolve(7L, owner))
        .thenReturn(new ProfileViewing(ProfileAccess.FULL, false, true));
    when(profileCounters.followers(1L)).thenReturn(5L);
    when(profileCounters.publishedTracks(1L)).thenReturn(3L);
    when(profileCounters.likesOfPublishedTracks(1L)).thenReturn(21L);

    PublicProfileDetails details = publicProfileService.getProfile(viewer(), PUBLIC_ID).details();

    assertEquals(5L, details.followersCount());
    assertEquals(3L, details.tracksCount());
    assertEquals(21L, details.likesCount());
    assertEquals(YearMonth.of(2025, 3), details.joinedAt());
  }

  @Test
  void avatarOfHiddenProfileIsNotServed() {
    User owner = User.builder().id(1L).publicId(PUBLIC_ID).avatarFileName("avatar.png").build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(owner);
    when(profileAccessResolver.resolve(7L, owner))
        .thenReturn(new ProfileViewing(ProfileAccess.HIDDEN, false, false));

    Optional<AvatarImage> avatar = publicProfileService.getAvatar(viewer(), PUBLIC_ID);

    assertTrue(avatar.isEmpty());
    verifyNoInteractions(avatarFiles);
  }

  private static CustomUserDetails viewer() {
    return new CustomUserDetails(User.builder().id(7L).build());
  }
}
