package com.radiofind.track.review;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.IllegalTrackStatusException;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackReviewServiceTest {

  @Mock
  private OwnTracks ownTracks;

  @Mock
  private RejectionReasons rejectionReasons;

  @InjectMocks
  private TrackReviewService trackReviewService;

  @Test
  void rejectedTrackExplainsWhy() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).status(TrackStatus.REJECTED).build();
    RejectionReasonResponse reason = new RejectionReasonResponse("Clipping", "Too loud", "Team", Instant.EPOCH);
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(rejectionReasons.reasonFor(track)).thenReturn(reason);

    RejectionReasonResponse explained = trackReviewService.getRejectionReason(ada, 42L);

    assertSame(reason, explained);
  }

  @Test
  void trackThatWasNotRejectedHasNoRejectionReason() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(ownTracks.require(ada, 42L)).thenReturn(Track.builder().id(42L).status(TrackStatus.APPROVED).build());

    assertThrows(IllegalTrackStatusException.class, () -> trackReviewService.getRejectionReason(ada, 42L));
  }
}
