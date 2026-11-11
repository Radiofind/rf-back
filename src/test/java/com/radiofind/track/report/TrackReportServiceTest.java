package com.radiofind.track.report;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.IllegalTrackStatusException;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.TrackReportedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackReportServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private TrackReportRepository trackReportRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Spy
  private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @InjectMocks
  private TrackReportService trackReportService;

  @Test
  void reportIsRecordedWithTrimmedCommentAndAdminsAreAlerted() {
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackReportRepository.insertIfAbsent(42L, 7L, "SPAM", "Same loop", NOW)).thenReturn(1);

    trackReportService.report(
        new CustomUserDetails(User.builder().id(7L).build()),
        42L,
        new TrackReportRequest(TrackReportReason.SPAM, "  Same loop  "));

    verify(eventPublisher).publishEvent(new TrackReportedEvent(track));
  }

  @Test
  void blankCommentIsStoredAsNone() {
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackReportRepository.insertIfAbsent(42L, 7L, "SPAM", null, NOW)).thenReturn(1);

    trackReportService.report(
        new CustomUserDetails(User.builder().id(7L).build()),
        42L,
        new TrackReportRequest(TrackReportReason.SPAM, "   "));

    verify(trackReportRepository).insertIfAbsent(42L, 7L, "SPAM", null, NOW);
  }

  @Test
  void secondReportOfTheSameTrackIsAConflict() {
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackReportRepository.insertIfAbsent(42L, 7L, "SPAM", null, NOW)).thenReturn(0);
    CustomUserDetails reporter = new CustomUserDetails(User.builder().id(7L).build());
    TrackReportRequest request = new TrackReportRequest(TrackReportReason.SPAM, null);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class, () -> trackReportService.report(reporter, 42L, request));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(eventPublisher, never()).publishEvent(any(TrackReportedEvent.class));
  }

  @Test
  void ownTrackCannotBeReported() {
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).owner(User.builder().id(7L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    CustomUserDetails owner = new CustomUserDetails(User.builder().id(7L).build());
    TrackReportRequest request = new TrackReportRequest(TrackReportReason.SPAM, null);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class, () -> trackReportService.report(owner, 42L, request));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(trackReportRepository);
  }

  @Test
  void otherReasonNeedsADescription() {
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    CustomUserDetails reporter = new CustomUserDetails(User.builder().id(7L).build());
    TrackReportRequest request = new TrackReportRequest(TrackReportReason.OTHER, " ");

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class, () -> trackReportService.report(reporter, 42L, request));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(trackReportRepository);
  }

  @Test
  void unpublishedTrackCannotBeReported() {
    Track track = Track.builder()
        .id(42L)
        .status(TrackStatus.PENDING_REVIEW)
        .owner(User.builder().id(1L).build())
        .build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    CustomUserDetails reporter = new CustomUserDetails(User.builder().id(7L).build());
    TrackReportRequest request = new TrackReportRequest(TrackReportReason.SPAM, null);

    assertThrows(IllegalTrackStatusException.class, () -> trackReportService.report(reporter, 42L, request));
  }

  @Test
  void unknownTrackIsNotFound() {
    when(trackRepository.findById(42L)).thenReturn(Optional.empty());
    CustomUserDetails reporter = new CustomUserDetails(User.builder().id(7L).build());
    TrackReportRequest request = new TrackReportRequest(TrackReportReason.SPAM, null);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class, () -> trackReportService.report(reporter, 42L, request));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }
}
