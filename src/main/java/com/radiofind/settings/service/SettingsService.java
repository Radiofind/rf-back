package com.radiofind.settings.service;

import com.radiofind.notification.entity.NotificationCategory;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.settings.dto.GeneralSettingsResponse;
import com.radiofind.settings.dto.NotificationSettingsResponse;
import com.radiofind.settings.dto.UpdateGeneralSettingsRequest;
import com.radiofind.settings.dto.UpdateNotificationSettingsRequest;
import com.radiofind.settings.entity.UserSettings;
import com.radiofind.settings.repository.UserSettingsRepository;
import com.radiofind.user.event.AccountDeletedEvent;
import com.radiofind.user.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    return toGeneralResponse(findSettings(userDetails.getUser().getId()));
  }

  @Transactional
  public GeneralSettingsResponse updateGeneralSettings(
      CustomUserDetails userDetails,
      UpdateGeneralSettingsRequest request) {
    UserSettings settings = findOrCreateSettings(userDetails.getUser().getId());

    settings.setLanguage(request.getLanguage());
    settings.setRadioMode(request.getRadioMode());
    settings.setProfileVisibility(request.getProfileVisibility());
    settings.setAudioQuality(request.getAudioQuality());
    settings.setAccentColor(request.getAccentColor());

    return toGeneralResponse(userSettingsRepository.save(settings));
  }

  @Transactional(readOnly = true)
  public NotificationSettingsResponse getNotificationSettings(CustomUserDetails userDetails) {
    return toNotificationResponse(findSettings(userDetails.getUser().getId()));
  }

  @Transactional
  public NotificationSettingsResponse updateNotificationSettings(
      CustomUserDetails userDetails,
      UpdateNotificationSettingsRequest request) {
    UserSettings settings = findOrCreateSettings(userDetails.getUser().getId());

    settings.setTrackUpdateNotifications(request.trackUpdates());
    settings.setRadioUpdateNotifications(request.radioUpdates());
    settings.setLikeNotifications(request.likes());

    return toNotificationResponse(userSettingsRepository.save(settings));
  }

  @Transactional(readOnly = true)
  public boolean receives(long userId, NotificationCategory category) {
    return findSettings(userId).receives(category);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedAccount(AccountDeletedEvent event) {
    userSettingsRepository.deleteByUserId(event.userId());
  }

  private UserSettings findSettings(long userId) {
    return userSettingsRepository.findByUserId(userId)
        .orElseGet(() -> UserSettings.builder().build());
  }

  private UserSettings findOrCreateSettings(long userId) {
    return userSettingsRepository.findByUserId(userId)
        .orElseGet(() -> UserSettings.builder()
            .user(userRepository.getReferenceById(userId))
            .build());
  }

  private GeneralSettingsResponse toGeneralResponse(UserSettings settings) {
    return GeneralSettingsResponse.builder()
        .language(settings.getLanguage())
        .radioMode(settings.isRadioMode())
        .profileVisibility(settings.getProfileVisibility())
        .audioQuality(settings.getAudioQuality())
        .accentColor(settings.getAccentColor())
        .build();
  }

  private NotificationSettingsResponse toNotificationResponse(UserSettings settings) {
    return new NotificationSettingsResponse(
        settings.isTrackUpdateNotifications(),
        settings.isRadioUpdateNotifications(),
        settings.isLikeNotifications());
  }
}
