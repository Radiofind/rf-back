package com.radiofind.track.schedule;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.IllegalTrackStatusException;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.ScheduleChangeRequestedEvent;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackScheduleServiceTest {

  private static final Instant AIRING = Instant.parse("2026-11-11T20:00:00Z");

  private static final Instant PREFERRED = Instant.parse("2026-11-20T18:00:00Z");

  @Mock
  private OwnTracks ownTracks;

  @Mock
  private PlaybackScheduler playbackScheduler;

  @Mock
  private ScheduleChangeRequestRepository scheduleChangeRequestRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private PlaybackScheduleService playbackScheduleService;

  @Test
  void scheduleShowsTheNextAiringAndTheChangeUnderReview() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).durationSeconds(214).build();
    ScheduleChangeRequest pending = ScheduleChangeRequest.builder()
        .preferredAt(PREFERRED)
        .comment("Prime time please")
        .createdAt(Instant.parse("2026-11-10T09:00:00Z"))
        .build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(playbackScheduler.nextAiring(track)).thenReturn(AIRING);
    when(scheduleChangeRequestRepository.findFirstByTrackIdAndStatus(42L, ScheduleChangeRequestStatus.PENDING))
        .thenReturn(Optional.of(pending));

    PlaybackScheduleResponse schedule = playbackScheduleService.getSchedule(ada, 42L);

    assertEquals(
        new PlaybackScheduleResponse(
            AIRING,
            214,
            new PendingScheduleChange(PREFERRED, "Prime time please", Instant.parse("2026-11-10T09:00:00Z"))),
        schedule);
  }

  @Test
  void requestedChangeWaitsForReviewAndIsAnnounced() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(scheduleChangeRequestRepository.findFirstByTrackIdAndStatus(42L, ScheduleChangeRequestStatus.PENDING))
        .thenReturn(Optional.empty());
    when(scheduleChangeRequestRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    ArgumentCaptor<ScheduleChangeRequest> saved = ArgumentCaptor.forClass(ScheduleChangeRequest.class);

    playbackScheduleService.requestChange(ada, 42L, new ScheduleChangeRequestBody(PREFERRED, "  Prime time  "));

    verify(scheduleChangeRequestRepository).saveAndFlush(saved.capture());
    assertEquals(ScheduleChangeRequestStatus.PENDING, saved.getValue().getStatus());
    assertEquals("Prime time", saved.getValue().getComment());
    verify(eventPublisher).publishEvent(new ScheduleChangeRequestedEvent(track));
  }

  @Test
  void secondChangeIsRefusedWhileOneIsUnderReview() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(ownTracks.require(ada, 42L)).thenReturn(Track.builder().id(42L).status(TrackStatus.APPROVED).build());
    when(scheduleChangeRequestRepository.findFirstByTrackIdAndStatus(42L, ScheduleChangeRequestStatus.PENDING))
        .thenReturn(Optional.of(ScheduleChangeRequest.builder().build()));
    ScheduleChangeRequestBody body = new ScheduleChangeRequestBody(PREFERRED, null);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class, () -> playbackScheduleService.requestChange(ada, 42L, body));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(scheduleChangeRequestRepository, never()).saveAndFlush(any());
  }

  @Test
  void unpublishedTrackHasNoSchedule() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(ownTracks.require(ada, 42L)).thenReturn(Track.builder().id(42L).status(TrackStatus.PENDING_REVIEW).build());

    assertThrows(IllegalTrackStatusException.class, () -> playbackScheduleService.getSchedule(ada, 42L));
  }
}
