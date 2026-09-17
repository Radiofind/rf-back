package com.radiofind.auth.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePasswordResetTokenResponse {

  private boolean valid;
}
