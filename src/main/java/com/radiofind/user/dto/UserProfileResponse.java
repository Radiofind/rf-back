package com.radiofind.user.dto;

import com.radiofind.artist.entity.ArtistType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

  private String name;

  private String surname;

  private LocalDate dateOfBirth;

  private ArtistType artistType;

  private String artistName;

  private String description;

  private LocalDateTime registeredAt;

  private String avatarUrl;
}
