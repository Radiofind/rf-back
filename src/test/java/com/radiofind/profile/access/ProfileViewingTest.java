package com.radiofind.profile.access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfileViewingTest {

  @ParameterizedTest(name = "{0} shows details={1}, avatar={2}, following offered={3}")
  @CsvSource({
      "FULL, true, true, true",
      "RESTRICTED, false, true, true",
      "HIDDEN, false, false, false"
  })
  void visitorSeesWhatTheAccessAllows(ProfileAccess access, boolean details, boolean avatar, boolean follow) {
    ProfileViewing viewing = new ProfileViewing(access, false, false);

    assertEquals(details, viewing.showsDetails());
    assertEquals(avatar, viewing.showsAvatar());
    assertEquals(follow, viewing.offersFollowing());
  }

  @Test
  void ownerIsNotOfferedToFollowThemselves() {
    assertFalse(new ProfileViewing(ProfileAccess.FULL, true, false).offersFollowing());
  }
}
