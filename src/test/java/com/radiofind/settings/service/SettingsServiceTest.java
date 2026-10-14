package com.radiofind.settings.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.settings.dto.GeneralSettingsResponse;
import com.radiofind.settings.dto.UpdateGeneralSettingsRequest;
import com.radiofind.settings.entity.AccentColor;
import com.radiofind.settings.entity.AudioQuality;
import com.radiofind.settings.entity.Language;
import com.radiofind.settings.entity.ProfileVisibility;
import com.radiofind.settings.entity.UserSettings;
import com.radiofind.settings.repository.UserSettingsRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

  private static final long USER_ID = 7L;

  private UserSettingsRepository userSettingsRepository;

  private UserRepository userRepository;

  private SettingsService settingsService;

  private final User user = User.builder().id(USER_ID).email("ada@example.com").build();

  private final CustomUserDetails userDetails = new CustomUserDetails(user);

  @BeforeEach
  void setUp() {
    userSettingsRepository = mock(UserSettingsRepository.class);
    userRepository = mock(UserRepository.class);
    settingsService = new SettingsService(userSettingsRepository, userRepository);

    when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private UpdateGeneralSettingsRequest request() {
    return UpdateGeneralSettingsRequest.builder()
        .language(Language.DE)
        .radioMode(false)
        .profileVisibility(ProfileVisibility.ONLY_ME)
        .notificationsEnabled(false)
        .audioQuality(AudioQuality.LOW)
        .accentColor(AccentColor.OCEAN)
        .build();
  }

  @Test
  void returnsDefaultsWhenUserHasNoSettings() {
    when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    GeneralSettingsResponse response = settingsService.getGeneralSettings(userDetails);

    assertEquals(GeneralSettingsResponse.builder()
        .language(Language.EN)
        .radioMode(true)
        .profileVisibility(ProfileVisibility.EVERYONE)
        .notificationsEnabled(true)
        .audioQuality(AudioQuality.HIGH)
        .accentColor(AccentColor.VIOLET)
        .build(), response);
    verify(userSettingsRepository, never()).save(any(UserSettings.class));
  }

  @Test
  void returnsStoredSettings() {
    when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(UserSettings.builder()
        .user(user)
        .language(Language.FR)
        .radioMode(false)
        .profileVisibility(ProfileVisibility.FOLLOWERS)
        .notificationsEnabled(false)
        .audioQuality(AudioQuality.NORMAL)
        .accentColor(AccentColor.PLUM)
        .build()));

    GeneralSettingsResponse response = settingsService.getGeneralSettings(userDetails);

    assertEquals(Language.FR, response.getLanguage());
    assertEquals(false, response.isRadioMode());
    assertEquals(ProfileVisibility.FOLLOWERS, response.getProfileVisibility());
    assertEquals(false, response.isNotificationsEnabled());
    assertEquals(AudioQuality.NORMAL, response.getAudioQuality());
    assertEquals(AccentColor.PLUM, response.getAccentColor());
  }

  @Test
  void createsSettingsForTheCurrentUserOnFirstUpdate() {
    User userReference = User.builder().id(USER_ID).build();
    when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(userRepository.getReferenceById(USER_ID)).thenReturn(userReference);

    GeneralSettingsResponse response = settingsService.updateGeneralSettings(userDetails, request());

    ArgumentCaptor<UserSettings> saved = ArgumentCaptor.forClass(UserSettings.class);
    verify(userSettingsRepository).save(saved.capture());

    assertSame(userReference, saved.getValue().getUser());
    assertEquals(Language.DE, response.getLanguage());
    assertEquals(false, response.isRadioMode());
    assertEquals(ProfileVisibility.ONLY_ME, response.getProfileVisibility());
    assertEquals(false, response.isNotificationsEnabled());
    assertEquals(AudioQuality.LOW, response.getAudioQuality());
    assertEquals(AccentColor.OCEAN, response.getAccentColor());
  }

  @Test
  void updatesExistingSettingsInPlace() {
    UserSettings existing = UserSettings.builder().id(3L).user(user).build();
    when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

    settingsService.updateGeneralSettings(userDetails, request());

    verify(userSettingsRepository).save(existing);
    verify(userRepository, never()).getReferenceById(any());
    assertEquals(3L, existing.getId());
    assertEquals(Language.DE, existing.getLanguage());
    assertEquals(AccentColor.OCEAN, existing.getAccentColor());
  }
}
