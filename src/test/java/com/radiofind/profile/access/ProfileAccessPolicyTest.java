package com.radiofind.profile.access;

import com.radiofind.settings.entity.ProfileVisibility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileAccessPolicyTest {

  @ParameterizedTest(name = "{0} viewed by a follower={1} is {2}")
  @CsvSource({
      "EVERYONE, false, FULL",
      "EVERYONE, true, FULL",
      "FOLLOWERS, false, RESTRICTED",
      "FOLLOWERS, true, FULL",
      "ONLY_ME, false, HIDDEN",
      "ONLY_ME, true, HIDDEN"
  })
  void accessFollowsTheOwnersVisibility(ProfileVisibility visibility, boolean viewerFollows, ProfileAccess expected) {
    assertEquals(expected, ProfileAccessPolicy.resolve(visibility, viewerFollows));
  }
}
