package com.radiofind.settings.dto;

import com.radiofind.settings.entity.AccentColor;
import com.radiofind.settings.entity.AudioQuality;
import com.radiofind.settings.entity.Language;
import com.radiofind.settings.entity.ProfileVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGeneralSettingsRequest {

  @NotNull
  private Language language;

  @NotNull
  private Boolean radioMode;

  @NotNull
  private ProfileVisibility profileVisibility;

  @NotNull
  private Boolean notificationsEnabled;

  @NotNull
  private AudioQuality audioQuality;

  @NotNull
  private AccentColor accentColor;
}
