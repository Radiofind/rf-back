package com.radiofind.profile.access;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.settings.entity.ProfileVisibility;
import com.radiofind.settings.service.SettingsService;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAccessResolverTest {

  @Mock
  private SettingsService settingsService;

  @Mock
  private FollowRepository followRepository;

  @InjectMocks
  private ProfileAccessResolver profileAccessResolver;

  @Test
  void ownerSeesTheirOwnProfileInFullWhateverTheVisibility() {
    ProfileViewing viewing = profileAccessResolver.resolve(7L, User.builder().id(7L).build());

    assertEquals(new ProfileViewing(ProfileAccess.FULL, true, false), viewing);
    verifyNoInteractions(settingsService, followRepository);
  }

  @Test
  void followerSeesAFollowersOnlyProfileInFull() {
    when(followRepository.existsByPair(7L, 1L)).thenReturn(true);
    when(settingsService.profileVisibilityOf(1L)).thenReturn(ProfileVisibility.FOLLOWERS);

    ProfileViewing viewing = profileAccessResolver.resolve(7L, User.builder().id(1L).build());

    assertEquals(new ProfileViewing(ProfileAccess.FULL, false, true), viewing);
  }

  @Test
  void strangerSeesAFollowersOnlyProfileRestricted() {
    when(followRepository.existsByPair(7L, 1L)).thenReturn(false);
    when(settingsService.profileVisibilityOf(1L)).thenReturn(ProfileVisibility.FOLLOWERS);

    ProfileViewing viewing = profileAccessResolver.resolve(7L, User.builder().id(1L).build());

    assertEquals(new ProfileViewing(ProfileAccess.RESTRICTED, false, false), viewing);
  }
}
