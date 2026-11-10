package com.radiofind.track.review;

import com.radiofind.track.entity.Track;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderRejectionReasons implements RejectionReasons {

  private static final String SUMMARY = "Audio quality below broadcast standard";

  private static final String DETAILS = "We hear clipping in the loud passages and the loudness changes noticeably "
      + "between sections. Please master the track to about -14 LUFS without clipping and resubmit it.";

  private static final String REVIEWER = "Radiofind review team";

  @Override
  public RejectionReasonResponse reasonFor(Track track) {
    return new RejectionReasonResponse(SUMMARY, DETAILS, REVIEWER, track.getUpdatedAt());
  }
}
