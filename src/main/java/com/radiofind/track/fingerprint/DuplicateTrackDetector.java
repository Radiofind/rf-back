package com.radiofind.track.fingerprint;

import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackFingerprint;
import com.radiofind.track.entity.TrackReviewFlagType;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.repository.TrackFingerprintRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DuplicateTrackDetector {

  private final TrackFingerprintRepository trackFingerprintRepository;

  private final double duplicateSimilarity;

  private final double possibleDuplicateSimilarity;

  private final int durationToleranceSeconds;

  private final int maxOffset;

  private final int minOverlap;

  public DuplicateTrackDetector(
      TrackFingerprintRepository trackFingerprintRepository,
      @Value("${app.fingerprint.duplicate-similarity}") double duplicateSimilarity,
      @Value("${app.fingerprint.possible-duplicate-similarity}") double possibleDuplicateSimilarity,
      @Value("${app.fingerprint.duration-tolerance-seconds}") int durationToleranceSeconds,
      @Value("${app.fingerprint.max-offset}") int maxOffset,
      @Value("${app.fingerprint.min-overlap}") int minOverlap) {
    this.trackFingerprintRepository = trackFingerprintRepository;
    this.duplicateSimilarity = duplicateSimilarity;
    this.possibleDuplicateSimilarity = possibleDuplicateSimilarity;
    this.durationToleranceSeconds = durationToleranceSeconds;
    this.maxOffset = maxOffset;
    this.minOverlap = minOverlap;
  }

  public DuplicateCheck check(Long ownerId, AudioFingerprint fingerprint) {
    List<TrackFingerprint> candidates = trackFingerprintRepository.findWithDurationBetween(
        fingerprint.durationSeconds() - durationToleranceSeconds,
        fingerprint.durationSeconds() + durationToleranceSeconds);

    List<ScoredTrack> similarTracks = candidates.stream()
        .map(candidate -> new ScoredTrack(candidate.getTrack(), FingerprintComparison.similarity(
            fingerprint.points(), candidate.getPoints(), maxOffset, minOverlap)))
        .filter(scored -> scored.similarity() >= possibleDuplicateSimilarity)
        .toList();

    Optional<Track> ownDuplicate = similarTracks.stream()
        .filter(scored -> isOwnActiveDuplicate(ownerId, scored))
        .map(ScoredTrack::track)
        .findFirst();

    List<TrackMatch> flaggedMatches = similarTracks.stream()
        .map(scored -> new TrackMatch(scored.track(), flagFor(ownerId, scored), scored.similarity()))
        .toList();

    return new DuplicateCheck(ownDuplicate, flaggedMatches);
  }

  private boolean isOwnActiveDuplicate(Long ownerId, ScoredTrack scored) {
    return isOwnedBy(ownerId, scored.track())
        && scored.similarity() >= duplicateSimilarity
        && scored.track().getStatus() != TrackStatus.REJECTED;
  }

  private TrackReviewFlagType flagFor(Long ownerId, ScoredTrack scored) {
    if (!isOwnedBy(ownerId, scored.track())) {
      return TrackReviewFlagType.MATCHES_OTHER_USERS_TRACK;
    }

    return scored.track().getStatus() == TrackStatus.REJECTED && scored.similarity() >= duplicateSimilarity
        ? TrackReviewFlagType.REUPLOAD_OF_REJECTED_TRACK
        : TrackReviewFlagType.RESEMBLES_OWN_TRACK;
  }

  private boolean isOwnedBy(Long ownerId, Track track) {
    return Objects.equals(ownerId, track.getOwner().getId());
  }

  private record ScoredTrack(Track track, double similarity) {
  }
}
