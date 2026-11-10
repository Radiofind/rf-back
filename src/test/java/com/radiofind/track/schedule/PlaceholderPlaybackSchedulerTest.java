package com.radiofind.track.schedule;

import com.radiofind.track.entity.Track;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceholderPlaybackSchedulerTest {

  @ParameterizedTest
  @CsvSource({
      "2026-11-10T08:00:00Z, 2026-11-10T20:00:00Z",
      "2026-11-10T20:00:00Z, 2026-11-11T20:00:00Z",
      "2026-11-10T23:30:00Z, 2026-11-11T20:00:00Z"
  })
  void trackAirsAtTheNextEveningSlot(Instant now, Instant expected) {
    PlaceholderPlaybackScheduler scheduler = new PlaceholderPlaybackScheduler(Clock.fixed(now, ZoneOffset.UTC));

    Instant airing = scheduler.nextAiring(Track.builder().id(42L).build());

    assertEquals(expected, airing);
  }
}
