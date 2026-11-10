package com.radiofind.track.schedule;

import java.time.Instant;

public record PlaybackScheduleResponse(Instant airsAt, Integer durationSeconds, PendingScheduleChange pendingChange) {
}
