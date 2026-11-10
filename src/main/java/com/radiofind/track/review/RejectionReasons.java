package com.radiofind.track.review;

import com.radiofind.track.entity.Track;

public interface RejectionReasons {

  RejectionReasonResponse reasonFor(Track track);
}
