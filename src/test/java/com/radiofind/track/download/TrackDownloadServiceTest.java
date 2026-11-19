package com.radiofind.track.download;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackDownloadServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private TrackDownloadRepository trackDownloadRepository;

  @Spy
  private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @InjectMocks
  private TrackDownloadService trackDownloadService;

  @Test
  void everyDownloadOfAnotherUsersTrackIsRecorded() {
    Track track = Track.builder().id(42L).owner(User.builder().id(1L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));

    trackDownloadService.recordDownload(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verify(trackDownloadRepository).insert(42L, 7L, NOW);
  }

  @Test
  void ownerDownloadingTheOwnTrackDoesNotCount() {
    Track track = Track.builder().id(42L).owner(User.builder().id(7L).build()).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));

    trackDownloadService.recordDownload(new CustomUserDetails(User.builder().id(7L).build()), 42L);

    verifyNoInteractions(trackDownloadRepository);
  }

  @Test
  void downloadOfAnUnknownTrackIsNotFound() {
    when(trackRepository.findById(42L)).thenReturn(Optional.empty());
    CustomUserDetails fan = new CustomUserDetails(User.builder().id(7L).build());

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> trackDownloadService.recordDownload(fan, 42L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }
}
