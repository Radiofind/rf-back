package com.radiofind.track.editing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EditTrackRequest {

  @NotBlank
  @Size(max = 150)
  private String title;

  @NotEmpty
  @Size(max = 3)
  private List<@NotBlank @Size(max = 100) String> genres;

  @Size(max = 1000)
  private String description;

  private boolean removeCover;
}
