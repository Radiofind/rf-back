package com.radiofind.track.fingerprint;

import com.radiofind.track.entity.Track;

import java.util.List;
import java.util.Optional;

public record DuplicateCheck(Optional<Track> ownDuplicate, List<TrackMatch> flaggedMatches) {
}
