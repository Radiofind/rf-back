package com.radiofind.settings.dto;

import com.radiofind.settings.entity.AccentColor;
import com.radiofind.settings.entity.AudioQuality;
import com.radiofind.settings.entity.Language;
import com.radiofind.settings.entity.ProfileVisibility;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralSettingsResponse {

  private Language language;

  private boolean radioMode;

  private ProfileVisibility profileVisibility;

  private boolean notificationsEnabled;

  private AudioQuality audioQuality;

  private AccentColor accentColor;
}
