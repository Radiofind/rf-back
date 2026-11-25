package com.radiofind.profile.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayNameTest {

  @Test
  void artistNameIsPublishedTrimmed() {
    User user = User.builder()
        .name("Ada")
        .surname("Lovelace")
        .artistProfile(ArtistProfile.builder().artistName("  Analytical Engine ").build())
        .build();

    assertEquals("Analytical Engine", DisplayName.of(user));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "", "   " })
  void withoutArtistNameOnlyTheSurnameInitialIsPublished(String artistName) {
    User user = User.builder()
        .name("Ada")
        .surname("Lovelace")
        .artistProfile(ArtistProfile.builder().artistName(artistName).build())
        .build();

    assertEquals("Ada L.", DisplayName.of(user));
  }

  @Test
  void userWithoutArtistProfileIsNamedByFirstNameAndSurnameInitial() {
    User user = User.builder().name("Ada").surname("Lovelace").build();

    assertEquals("Ada L.", DisplayName.of(user));
  }
}
