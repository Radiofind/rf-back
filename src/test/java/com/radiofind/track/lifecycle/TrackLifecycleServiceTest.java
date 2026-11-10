package com.radiofind.track.lifecycle;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.TrackResubmittedEvent;
import com.radiofind.track.event.TrackSubmissionCancelledEvent;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackLifecycleServiceTest {

  @Mock
  private OwnTracks ownTracks;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private TrackLifecycleService trackLifecycleService;

  @Test
  void withdrawnTrackBecomesDraftAndIsAnnounced() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).status(TrackStatus.PENDING_REVIEW).build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);

    trackLifecycleService.cancelSubmission(ada, 42L);

    assertEquals(TrackStatus.DRAFT, track.getStatus());
    verify(eventPublisher).publishEvent(new TrackSubmissionCancelledEvent(track));
  }

  @Test
  void resubmittedTrackGoesToReviewAndIsAnnounced() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).status(TrackStatus.REJECTED).build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);

    trackLifecycleService.submitForReview(ada, 42L);

    assertEquals(TrackStatus.PENDING_REVIEW, track.getStatus());
    verify(eventPublisher).publishEvent(new TrackResubmittedEvent(track));
  }
}
