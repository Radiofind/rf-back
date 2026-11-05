package com.radiofind.activity.dto;

import com.radiofind.activity.entity.ActivityType;
import com.radiofind.track.dto.TrackSummaryResponse;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {

  private Long id;

  private ActivityType type;

  private Instant createdAt;

  private TrackSummaryResponse track;
}
