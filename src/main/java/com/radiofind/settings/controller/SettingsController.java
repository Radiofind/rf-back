package com.radiofind.settings.controller;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.settings.dto.GeneralSettingsResponse;
import com.radiofind.settings.dto.UpdateGeneralSettingsRequest;
import com.radiofind.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/settings")
@RequiredArgsConstructor
public class SettingsController {

  private final SettingsService settingsService;

  @GetMapping("/general")
  public GeneralSettingsResponse getGeneralSettings(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return settingsService.getGeneralSettings(userDetails);
  }

  @PutMapping("/general")
  public GeneralSettingsResponse updateGeneralSettings(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateGeneralSettingsRequest request) {
    return settingsService.updateGeneralSettings(userDetails, request);
  }
}
