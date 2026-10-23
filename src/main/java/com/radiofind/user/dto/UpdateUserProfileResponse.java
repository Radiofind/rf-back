package com.radiofind.user.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileResponse {

  private UserProfileResponse profile;

  private boolean emailChanged;

  private boolean requiresReauthentication;
}
