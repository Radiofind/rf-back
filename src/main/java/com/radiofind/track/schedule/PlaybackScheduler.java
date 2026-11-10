package com.radiofind.track.schedule;

import com.radiofind.track.entity.Track;

import java.time.Instant;

public interface PlaybackScheduler {

  Instant nextAiring(Track track);
}
