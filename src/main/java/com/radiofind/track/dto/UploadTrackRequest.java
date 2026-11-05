package com.radiofind.track.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UploadTrackRequest {

  @NotBlank
  @Size(max = 150)
  private String title;

  @NotBlank
  @Size(max = 100)
  private String genre;

  @Size(max = 100)
  private String subgenre;

  @Size(max = 1000)
  private String description;
}
