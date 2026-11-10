package com.radiofind.track.review;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.service.OwnTracks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackReviewService {

  private final OwnTracks ownTracks;

  private final RejectionReasons rejectionReasons;

  @Transactional(readOnly = true)
  public RejectionReasonResponse getRejectionReason(CustomUserDetails userDetails, Long trackId) {
    Track track = ownTracks.require(userDetails, trackId);
    track.requireStatus(TrackStatus.REJECTED, "explained as rejected");

    return rejectionReasons.reasonFor(track);
  }
}
