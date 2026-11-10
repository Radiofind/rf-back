package com.radiofind.track.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackLikeRepository;
import com.radiofind.track.repository.TrackListenRepository;
import com.radiofind.track.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class TrackEngagementService {

  private final TrackRepository trackRepository;

  private final TrackListenRepository trackListenRepository;

  private final TrackLikeRepository trackLikeRepository;

  private final Clock clock;

  @Transactional
  public void recordPlay(CustomUserDetails userDetails, Long trackId) {
    Track track = findTrack(trackId);
    Long listenerId = userDetails.getUser().getId();

    if (track.getOwner().getId().equals(listenerId)) {
      return;
    }

    if (trackListenRepository.insertIfAbsent(trackId, listenerId, clock.instant()) > 0) {
      trackRepository.incrementPlays(trackId);
    }
  }

  @Transactional
  public void like(CustomUserDetails userDetails, Long trackId) {
    findTrack(trackId);

    if (trackLikeRepository.insertIfAbsent(trackId, userDetails.getUser().getId(), clock.instant()) > 0) {
      trackRepository.incrementLikes(trackId);
    }
  }

  @Transactional
  public void unlike(CustomUserDetails userDetails, Long trackId) {
    findTrack(trackId);

    if (trackLikeRepository.deleteByTrackAndUser(trackId, userDetails.getUser().getId()) > 0) {
      trackRepository.decrementLikes(trackId);
    }
  }

  private Track findTrack(Long trackId) {
    return trackRepository.findById(trackId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
  }
}
