package com.radiofind.profile.service;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.profile.access.ProfileAccess;
import com.radiofind.profile.access.ProfileAccessResolver;
import com.radiofind.profile.access.ProfileViewing;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileFollowServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  private static final UUID PUBLIC_ID = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");

  @Mock
  private PublicProfiles publicProfiles;

  @Mock
  private ProfileAccessResolver profileAccessResolver;

  @Mock
  private FollowRepository followRepository;

  @Spy
  private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @InjectMocks
  private ProfileFollowService profileFollowService;

  @Test
  void followerStartsFollowingAProfileThatOffersIt() {
    User followee = User.builder().id(1L).build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(followee);
    when(profileAccessResolver.resolve(7L, followee))
        .thenReturn(new ProfileViewing(ProfileAccess.RESTRICTED, false, false));

    profileFollowService.follow(new CustomUserDetails(User.builder().id(7L).build()), PUBLIC_ID);

    verify(followRepository).insertIfAbsent(7L, 1L, NOW);
  }

  @Test
  void rejectsFollowingYourself() {
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(User.builder().id(7L).build());
    CustomUserDetails self = new CustomUserDetails(User.builder().id(7L).build());

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> profileFollowService.follow(self, PUBLIC_ID));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(followRepository);
  }

  @Test
  void rejectsFollowingAHiddenProfile() {
    User followee = User.builder().id(1L).build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(followee);
    when(profileAccessResolver.resolve(7L, followee))
        .thenReturn(new ProfileViewing(ProfileAccess.HIDDEN, false, false));
    CustomUserDetails follower = new CustomUserDetails(User.builder().id(7L).build());

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> profileFollowService.follow(follower, PUBLIC_ID));

    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verifyNoInteractions(followRepository);
  }

  @Test
  void unfollowingIsAllowedEvenWhenTheProfileBecameHidden() {
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(User.builder().id(1L).build());

    profileFollowService.unfollow(new CustomUserDetails(User.builder().id(7L).build()), PUBLIC_ID);

    verify(followRepository).deleteByPair(7L, 1L);
    verifyNoInteractions(profileAccessResolver);
  }
}
