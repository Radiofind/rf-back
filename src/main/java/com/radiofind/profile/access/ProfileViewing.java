package com.radiofind.profile.access;

public record ProfileViewing(ProfileAccess access, boolean own, boolean following) {

  public boolean showsDetails() {
    return access == ProfileAccess.FULL;
  }

  public boolean showsAvatar() {
    return access != ProfileAccess.HIDDEN;
  }

  public boolean offersFollowing() {
    return !own && access != ProfileAccess.HIDDEN;
  }
}
