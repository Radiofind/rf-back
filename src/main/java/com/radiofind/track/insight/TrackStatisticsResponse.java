package com.radiofind.track.insight;

import java.time.Instant;

public record TrackStatisticsResponse(
    long playsCount,
    long likesCount,
    long uniqueListeners,
    long radioAirings,
    long listenersLast7Days,
    long listenersLast30Days,
    Instant lastListenedAt) {
}
