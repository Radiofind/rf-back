package com.radiofind.auth.dto;

import com.radiofind.artist.entity.ArtistType;
import lombok.Data;

@Data
public class ArtistInformationDto {

  private ArtistType typeOfArtist;

  private String artistName;

  private String description;

}
