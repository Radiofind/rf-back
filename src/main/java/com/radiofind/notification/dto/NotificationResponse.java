package com.radiofind.notification.dto;

import com.radiofind.notification.entity.NotificationType;
import com.radiofind.track.dto.TrackSummaryResponse;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

  private Long id;

  private NotificationType type;

  private boolean read;

  private Instant createdAt;

  private TrackSummaryResponse track;
}
