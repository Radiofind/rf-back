package com.radiofind.track.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.repository.TrackLikeRepository;
import com.radiofind.track.repository.TrackListenRepository;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackEngagementServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private TrackListenRepository trackListenRepository;

  @Mock
  private TrackLikeRepository trackLikeRepository;

  @Spy
  private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @InjectMocks
  private TrackEngagementService trackEngagementService;

  @Test
  void firstQualifiedListenOfAListenerCountsAsPlay() {
    Track track = Track.builder().id(42L).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackListenRepository.insertIfAbsent(42L, 7L, NOW)).thenReturn(1);

    trackEngagementService.recordPlay(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository).incrementPlays(42L);
  }

  @Test
  void repeatedListenOfTheSameListenerDoesNotCountAgain() {
    Track track = Track.builder().id(42L).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackListenRepository.insertIfAbsent(42L, 7L, NOW)).thenReturn(0);

    trackEngagementService.recordPlay(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository, never()).incrementPlays(42L);
  }

  @Test
  void ownerListeningToTheOwnTrackDoesNotCount() {
    Track track = Track.builder().id(42L).owner(User.builder().id(7L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));

    trackEngagementService.recordPlay(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verifyNoInteractions(trackListenRepository);
    verify(trackRepository, never()).incrementPlays(42L);
  }

  @Test
  void listenToAnUnknownTrackIsNotFound() {
    when(trackRepository.findById(42L)).thenReturn(Optional.empty());

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> trackEngagementService.recordPlay(new CustomUserDetails(User.builder().id(7L).build()), 42L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }

  @Test
  void ownerMayLikeTheOwnTrack() {
    Track track = Track.builder()
        .id(42L)
        .owner(User.builder().id(7L).build())
        .status(TrackStatus.APPROVED)
        .build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackLikeRepository.insertIfAbsent(42L, 7L, NOW)).thenReturn(1);

    trackEngagementService.like(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository).incrementLikes(42L);
  }

  @Test
  void likingTwiceCountsOnce() {
    when(trackRepository.findById(42L))
        .thenReturn(Optional.of(Track.builder().id(42L).status(TrackStatus.APPROVED).build()));
    when(trackLikeRepository.insertIfAbsent(42L, 7L, NOW)).thenReturn(0);

    trackEngagementService.like(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository, never()).incrementLikes(42L);
  }

  @Test
  void trackInReviewCannotBeLiked() {
    when(trackRepository.findById(42L))
        .thenReturn(Optional.of(Track.builder().id(42L).status(TrackStatus.PENDING_REVIEW).build()));

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> trackEngagementService.like(new CustomUserDetails(User.builder().id(7L).build()), 42L));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verifyNoInteractions(trackLikeRepository);
  }

  @Test
  void likeOfATrackInReviewCanStillBeRemoved() {
    when(trackRepository.findById(42L))
        .thenReturn(Optional.of(Track.builder().id(42L).status(TrackStatus.PENDING_REVIEW).build()));
    when(trackLikeRepository.deleteByTrackAndUser(42L, 7L)).thenReturn(1);

    trackEngagementService.unlike(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository).decrementLikes(42L);
  }

  @Test
  void removingALikeLowersTheCount() {
    when(trackRepository.findById(42L)).thenReturn(Optional.of(Track.builder().id(42L).build()));
    when(trackLikeRepository.deleteByTrackAndUser(42L, 7L)).thenReturn(1);

    trackEngagementService.unlike(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository).decrementLikes(42L);
  }

  @Test
  void removingAMissingLikeChangesNothing() {
    when(trackRepository.findById(42L)).thenReturn(Optional.of(Track.builder().id(42L).build()));
    when(trackLikeRepository.deleteByTrackAndUser(42L, 7L)).thenReturn(0);

    trackEngagementService.unlike(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackRepository, never()).decrementLikes(42L);
  }
}
