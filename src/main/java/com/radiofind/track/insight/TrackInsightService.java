package com.radiofind.track.insight;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackListenRepository;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.track.service.TrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TrackInsightService {

  private static final Duration WEEK = Duration.ofDays(7);

  private static final Duration MONTH = Duration.ofDays(30);

  private final OwnTracks ownTracks;

  private final TrackMapper trackMapper;

  private final TrackListenRepository trackListenRepository;

  private final Clock clock;

  @Transactional(readOnly = true)
  public TrackDetailsResponse getDetails(CustomUserDetails userDetails, Long trackId) {
    return trackMapper.toDetails(ownTracks.require(userDetails, trackId));
  }

  @Transactional(readOnly = true)
  public TrackStatisticsResponse getStatistics(CustomUserDetails userDetails, Long trackId) {
    Track track = ownTracks.require(userDetails, trackId);
    Instant now = clock.instant();
    long uniqueListeners = trackListenRepository.countByTrackId(trackId);

    return new TrackStatisticsResponse(
        track.getPlaysCount(),
        track.getLikesCount(),
        uniqueListeners,
        Math.max(track.getPlaysCount() - uniqueListeners, 0),
        trackListenRepository.countByTrackIdAndCreatedAtAfter(trackId, now.minus(WEEK)),
        trackListenRepository.countByTrackIdAndCreatedAtAfter(trackId, now.minus(MONTH)),
        trackListenRepository.findLastListenedAt(trackId).orElse(null));
  }
}
