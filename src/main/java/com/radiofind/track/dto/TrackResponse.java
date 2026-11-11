package com.radiofind.track.dto;

import com.radiofind.storage.service.AudioType;
import com.radiofind.track.entity.TrackStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackResponse {

  private Long id;

  private String title;

  private String artistName;

  private List<String> genres;

  private String description;

  private TrackStatus status;

  private AudioType audioFormat;

  private long audioSizeBytes;

  private Integer durationSeconds;

  private String coverUrl;

  private String audioUrl;

  private long playsCount;

  private long likesCount;

  private boolean liked;

  private boolean own;

  private Instant createdAt;

  private Instant likedAt;
}
