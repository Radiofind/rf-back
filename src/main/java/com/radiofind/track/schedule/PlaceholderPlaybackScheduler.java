package com.radiofind.track.schedule;

import com.radiofind.track.entity.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class PlaceholderPlaybackScheduler implements PlaybackScheduler {

  private static final LocalTime AIRTIME = LocalTime.of(20, 0);

  private final Clock clock;

  @Override
  public Instant nextAiring(Track track) {
    ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
    ZonedDateTime today = now.with(AIRTIME);

    return (today.isAfter(now) ? today : today.plusDays(1)).toInstant();
  }
}
