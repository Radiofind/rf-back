package com.radiofind.user.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {

  private String name;

  private String surname;

  private String email;
}
