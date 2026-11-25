package com.radiofind.profile.access;

import com.radiofind.follow.repository.FollowRepository;
import com.radiofind.settings.service.SettingsService;
import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileAccessResolver {

  private final SettingsService settingsService;

  private final FollowRepository followRepository;

  public ProfileViewing resolve(long viewerId, User owner) {
    if (owner.getId() == viewerId) {
      return new ProfileViewing(ProfileAccess.FULL, true, false);
    }

    boolean following = followRepository.existsByPair(viewerId, owner.getId());
    ProfileAccess access = ProfileAccessPolicy.resolve(
        settingsService.profileVisibilityOf(owner.getId()),
        following);

    return new ProfileViewing(access, false, following);
  }
}
