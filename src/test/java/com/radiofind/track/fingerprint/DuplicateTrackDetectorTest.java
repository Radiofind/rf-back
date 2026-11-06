package com.radiofind.track.fingerprint;

import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackFingerprint;
import com.radiofind.track.entity.TrackReviewFlagType;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.repository.TrackFingerprintRepository;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateTrackDetectorTest {

  private static final int[] SONG = new Random(1).ints(200).toArray();

  @Mock
  private TrackFingerprintRepository trackFingerprintRepository;

  @Test
  void looksOnlyAtTracksOfSimilarLength() {
    DuplicateTrackDetector detector = new DuplicateTrackDetector(trackFingerprintRepository, 0.85, 0.7, 15, 10, 40);
    when(trackFingerprintRepository.findWithDurationBetween(185, 215)).thenReturn(List.of());

    detector.check(7L, new AudioFingerprint(200, SONG));

    verify(trackFingerprintRepository).findWithDurationBetween(185, 215);
  }

  @Test
  void blocksATrackTheUserAlreadyHasOnThePlatform() {
    DuplicateTrackDetector detector = new DuplicateTrackDetector(trackFingerprintRepository, 0.85, 0.7, 15, 10, 40);
    Track earlier = Track.builder().id(41L).owner(User.builder().id(7L).build()).status(TrackStatus.APPROVED).build();
    when(trackFingerprintRepository.findWithDurationBetween(185, 215))
        .thenReturn(List.of(TrackFingerprint.builder().track(earlier).points(SONG.clone()).build()));

    DuplicateCheck check = detector.check(7L, new AudioFingerprint(200, SONG));

    assertEquals(Optional.of(earlier), check.ownDuplicate());
  }

  @Test
  void letsTheUserUploadAgainAfterARejectionButTellsTheReviewer() {
    DuplicateTrackDetector detector = new DuplicateTrackDetector(trackFingerprintRepository, 0.85, 0.7, 15, 10, 40);
    Track rejected = Track.builder().id(41L).owner(User.builder().id(7L).build()).status(TrackStatus.REJECTED).build();
    when(trackFingerprintRepository.findWithDurationBetween(185, 215))
        .thenReturn(List.of(TrackFingerprint.builder().track(rejected).points(SONG.clone()).build()));

    DuplicateCheck check = detector.check(7L, new AudioFingerprint(200, SONG));

    assertEquals(Optional.empty(), check.ownDuplicate());
    assertEquals(List.of(new TrackMatch(rejected, TrackReviewFlagType.REUPLOAD_OF_REJECTED_TRACK, 1.0)),
        check.flaggedMatches());
  }

  @Test
  void letsAnotherUsersCopyThroughButTellsTheReviewer() {
    DuplicateTrackDetector detector = new DuplicateTrackDetector(trackFingerprintRepository, 0.85, 0.7, 15, 10, 40);
    Track foreign = Track.builder().id(41L).owner(User.builder().id(8L).build()).status(TrackStatus.APPROVED).build();
    when(trackFingerprintRepository.findWithDurationBetween(185, 215))
        .thenReturn(List.of(TrackFingerprint.builder().track(foreign).points(SONG.clone()).build()));

    DuplicateCheck check = detector.check(7L, new AudioFingerprint(200, SONG));

    assertEquals(Optional.empty(), check.ownDuplicate());
    assertEquals(List.of(new TrackMatch(foreign, TrackReviewFlagType.MATCHES_OTHER_USERS_TRACK, 1.0)),
        check.flaggedMatches());
  }

  @Test
  void flagsAPartlyMatchingOwnTrackInsteadOfBlockingIt() {
    DuplicateTrackDetector detector = new DuplicateTrackDetector(trackFingerprintRepository, 0.85, 0.7, 15, 10, 40);
    Track remixed = Track.builder().id(41L).owner(User.builder().id(7L).build()).status(TrackStatus.APPROVED).build();
    int[] remix = SONG.clone();
    for (int i = 0; i < remix.length; i++) {
      remix[i] ^= 0x0000_00FF;
    }
    when(trackFingerprintRepository.findWithDurationBetween(185, 215))
        .thenReturn(List.of(TrackFingerprint.builder().track(remixed).points(remix).build()));

    DuplicateCheck check = detector.check(7L, new AudioFingerprint(200, SONG));

    assertEquals(Optional.empty(), check.ownDuplicate());
    assertEquals(TrackReviewFlagType.RESEMBLES_OWN_TRACK, check.flaggedMatches().getFirst().flag());
  }

  @Test
  void ignoresUnrelatedTracks() {
    DuplicateTrackDetector detector = new DuplicateTrackDetector(trackFingerprintRepository, 0.85, 0.7, 15, 10, 40);
    Track other = Track.builder().id(41L).owner(User.builder().id(7L).build()).status(TrackStatus.APPROVED).build();
    when(trackFingerprintRepository.findWithDurationBetween(185, 215)).thenReturn(List.of(
        TrackFingerprint.builder().track(other).points(new Random(2).ints(200).toArray()).build()));

    DuplicateCheck check = detector.check(7L, new AudioFingerprint(200, SONG));

    assertEquals(new DuplicateCheck(Optional.empty(), List.of()), check);
  }
}
