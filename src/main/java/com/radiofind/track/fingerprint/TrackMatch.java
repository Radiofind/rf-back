package com.radiofind.track.fingerprint;

import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackReviewFlagType;

public record TrackMatch(Track track, TrackReviewFlagType flag, double similarity) {
}
