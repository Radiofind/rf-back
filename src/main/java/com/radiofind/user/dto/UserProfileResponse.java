package com.radiofind.user.dto;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import com.radiofind.artist.entity.ArtistType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

  private String name;

  private String surname;

  private String email;

  private String recoveryEmail;

  private LocalDate dateOfBirth;

  private ArtistType artistType;

  private String artistName;

  private List<String> mainGenres;

  private List<String> additionalGenres;

  private String shortDescription;

  private String description;

  private ArtistSocialLinksDto socialLinks;

  private LocalDateTime registeredAt;

  private String avatarUrl;
}
