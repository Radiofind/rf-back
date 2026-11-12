package com.radiofind.track.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.context.event.EventListener;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.event.TrackLikedEvent;
import com.radiofind.user.event.AccountDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.IllegalTrackStatusException;
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

  private final ApplicationEventPublisher eventPublisher;

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
    Track track = findTrack(trackId);

    if (!track.acceptsLikes()) {
      throw new IllegalTrackStatusException("liked", track.getStatus());
    }

    Long likerId = userDetails.getUser().getId();

    if (trackLikeRepository.insertIfAbsent(trackId, likerId, clock.instant()) == 0) {
      return;
    }

    trackRepository.incrementLikes(trackId);

    if (!track.getOwner().getId().equals(likerId)) {
      eventPublisher.publishEvent(new TrackLikedEvent(track));
    }
  }

  @Transactional
  public void unlike(CustomUserDetails userDetails, Long trackId) {
    findTrack(trackId);

    if (trackLikeRepository.deleteByTrackAndUser(trackId, userDetails.getUser().getId()) > 0) {
      trackRepository.decrementLikes(trackId);
    }
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedTrack(TrackDeletedEvent event) {
    trackLikeRepository.deleteByTrackId(event.trackId());
    trackListenRepository.deleteByTrackId(event.trackId());
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedAccount(AccountDeletedEvent event) {
    trackRepository.decrementLikesOfTracksLikedBy(event.userId());
    trackLikeRepository.deleteByUserId(event.userId());
    trackListenRepository.deleteByListenerId(event.userId());
  }

  private Track findTrack(Long trackId) {
    return trackRepository.findById(trackId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
  }
}
