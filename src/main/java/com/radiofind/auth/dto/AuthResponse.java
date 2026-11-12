package com.radiofind.auth.dto;

import com.radiofind.auth.entity.TwoFactorMethod;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  private String token;

  private boolean requiresTwoFactor;

  private String challengeId;

  private TwoFactorMethod twoFactorMethod;

  private String destination;
}
