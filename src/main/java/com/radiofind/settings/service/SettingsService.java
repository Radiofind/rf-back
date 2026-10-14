package com.radiofind.settings.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.settings.dto.GeneralSettingsResponse;
import com.radiofind.settings.dto.UpdateGeneralSettingsRequest;
import com.radiofind.settings.entity.UserSettings;
import com.radiofind.settings.repository.UserSettingsRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

  private final UserSettingsRepository userSettingsRepository;
  private final UserRepository userRepository;

  SettingsService(UserSettingsRepository userSettingsRepository, UserRepository userRepository) {
    this.userSettingsRepository = userSettingsRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public GeneralSettingsResponse getGeneralSettings(CustomUserDetails userDetails) {
    User user = userDetails.getUser();

    UserSettings settings = userSettingsRepository.findByUserId(user.getId())
        .orElseGet(() -> UserSettings.builder().build());

    return toResponse(settings);
  }

  @Transactional
  public GeneralSettingsResponse updateGeneralSettings(
      CustomUserDetails userDetails,
      UpdateGeneralSettingsRequest request) {
    User user = userDetails.getUser();

    UserSettings settings = userSettingsRepository.findByUserId(user.getId())
        .orElseGet(() -> UserSettings.builder()
            .user(userRepository.getReferenceById(user.getId()))
            .build());

    settings.setLanguage(request.getLanguage());
    settings.setRadioMode(request.getRadioMode());
    settings.setProfileVisibility(request.getProfileVisibility());
    settings.setNotificationsEnabled(request.getNotificationsEnabled());
    settings.setAudioQuality(request.getAudioQuality());
    settings.setAccentColor(request.getAccentColor());

    return toResponse(userSettingsRepository.save(settings));
  }

  private GeneralSettingsResponse toResponse(UserSettings settings) {
    return GeneralSettingsResponse.builder()
        .language(settings.getLanguage())
        .radioMode(settings.isRadioMode())
        .profileVisibility(settings.getProfileVisibility())
        .notificationsEnabled(settings.isNotificationsEnabled())
        .audioQuality(settings.getAudioQuality())
        .accentColor(settings.getAccentColor())
        .build();
  }
}
