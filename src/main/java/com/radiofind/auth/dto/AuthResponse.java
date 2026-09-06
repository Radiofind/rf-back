package com.radiofind.auth.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  private String token;

  private boolean requiresTwoFactor;

  private String challengeId;
}
