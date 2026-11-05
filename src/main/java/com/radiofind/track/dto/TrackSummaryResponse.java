package com.radiofind.track.dto;

import com.radiofind.track.entity.TrackStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackSummaryResponse {

  private Long id;

  private String title;

  private String artistName;

  private String coverUrl;

  private TrackStatus status;
}
