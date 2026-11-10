package com.radiofind.track.fingerprint;

import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.repository.TrackFingerprintRepository;
import com.radiofind.track.repository.TrackReviewFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeletedTrackFingerprints {

  private final TrackFingerprintRepository trackFingerprintRepository;

  private final TrackReviewFlagRepository trackReviewFlagRepository;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forget(TrackDeletedEvent event) {
    trackReviewFlagRepository.deleteInvolving(event.trackId());
    trackFingerprintRepository.deleteByTrackId(event.trackId());
  }
}
