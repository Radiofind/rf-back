package com.radiofind.auth.dto;

import com.radiofind.artist.entity.ArtistType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtistInformationDto {

  private ArtistType typeOfArtist;

  @Size(max = 150)
  private String artistName;

  @Size(max = 3000)
  private String description;

}
