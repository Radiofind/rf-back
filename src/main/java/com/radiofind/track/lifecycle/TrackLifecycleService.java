package com.radiofind.track.lifecycle;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackResubmittedEvent;
import com.radiofind.track.event.TrackSubmissionCancelledEvent;
import com.radiofind.track.service.OwnTracks;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackLifecycleService {

  private final OwnTracks ownTracks;

  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void cancelSubmission(CustomUserDetails userDetails, Long trackId) {
    Track track = ownTracks.require(userDetails, trackId);

    track.cancelSubmission();
    eventPublisher.publishEvent(new TrackSubmissionCancelledEvent(track));
  }

  @Transactional
  public void submitForReview(CustomUserDetails userDetails, Long trackId) {
    Track track = ownTracks.require(userDetails, trackId);

    track.submitForReview();
    eventPublisher.publishEvent(new TrackResubmittedEvent(track));
  }
}
