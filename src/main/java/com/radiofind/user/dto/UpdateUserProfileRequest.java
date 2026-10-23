package com.radiofind.user.dto;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import com.radiofind.artist.entity.ArtistType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserProfileRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @NotBlank
  @Size(max = 100)
  private String surname;

  @NotBlank
  @Email
  @Size(max = 254)
  private String email;

  @Email
  @Size(max = 254)
  private String recoveryEmail;

  @Size(max = 150)
  private String artistName;

  private ArtistType artistType;

  @Size(max = 500)
  private String shortDescription;

  @Size(max = 3000)
  private String description;

  private List<@Size(max = 100) String> mainGenres;

  private List<@Size(max = 100) String> additionalGenres;

  @Valid
  private ArtistSocialLinksDto socialLinks;
}
