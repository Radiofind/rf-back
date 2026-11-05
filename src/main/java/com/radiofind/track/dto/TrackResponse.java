package com.radiofind.track.dto;

import com.radiofind.storage.service.AudioType;
import com.radiofind.track.entity.TrackStatus;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackResponse {

  private Long id;

  private String title;

  private String artistName;

  private String genre;

  private String subgenre;

  private String description;

  private TrackStatus status;

  private AudioType audioFormat;

  private long audioSizeBytes;

  private String coverUrl;

  private Instant createdAt;
}
