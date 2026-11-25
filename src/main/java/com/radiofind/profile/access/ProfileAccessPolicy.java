package com.radiofind.profile.access;

import com.radiofind.settings.entity.ProfileVisibility;

public final class ProfileAccessPolicy {

  private ProfileAccessPolicy() {
  }

  public static ProfileAccess resolve(ProfileVisibility visibility, boolean viewerFollows) {
    return switch (visibility) {
      case EVERYONE -> ProfileAccess.FULL;
      case FOLLOWERS -> viewerFollows ? ProfileAccess.FULL : ProfileAccess.RESTRICTED;
      case ONLY_ME -> ProfileAccess.HIDDEN;
    };
  }
}
