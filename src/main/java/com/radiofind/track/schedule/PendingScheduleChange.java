package com.radiofind.track.schedule;

import java.time.Instant;

public record PendingScheduleChange(Instant preferredAt, String comment, Instant requestedAt) {

  static PendingScheduleChange of(ScheduleChangeRequest request) {
    return new PendingScheduleChange(request.getPreferredAt(), request.getComment(), request.getCreatedAt());
  }
}
