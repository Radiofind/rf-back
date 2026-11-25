package com.radiofind.profile.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.user.entity.User;

public final class DisplayName {

  private DisplayName() {
  }

  public static String of(User user) {
    ArtistProfile artistProfile = user.getArtistProfile();
    String artistName = artistProfile != null ? artistProfile.getArtistName() : null;

    if (artistName != null && !artistName.isBlank()) {
      return artistName.trim();
    }

    return user.getName().trim() + " " + user.getSurname().trim().charAt(0) + ".";
  }
}
