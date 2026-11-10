package com.radiofind.track.insight;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackListenRepository;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.track.service.TrackMapper;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackInsightServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

  @Mock
  private OwnTracks ownTracks;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @Mock
  private TrackListenRepository trackListenRepository;

  @Spy
  private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @InjectMocks
  private TrackInsightService trackInsightService;

  @Test
  void statisticsSplitPlaysIntoListenersAndRadioAirings() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(ownTracks.require(ada, 42L)).thenReturn(Track.builder().id(42L).playsCount(12).likesCount(4).build());
    when(trackListenRepository.countByTrackId(42L)).thenReturn(9L);
    when(trackListenRepository.countByTrackIdAndCreatedAtAfter(42L, Instant.parse("2026-11-03T12:00:00Z")))
        .thenReturn(2L);
    when(trackListenRepository.countByTrackIdAndCreatedAtAfter(42L, Instant.parse("2026-10-11T12:00:00Z")))
        .thenReturn(5L);
    when(trackListenRepository.findLastListenedAt(42L)).thenReturn(Optional.of(Instant.parse("2026-11-09T08:00:00Z")));

    TrackStatisticsResponse statistics = trackInsightService.getStatistics(ada, 42L);

    assertEquals(
        new TrackStatisticsResponse(12, 4, 9, 3, 2, 5, Instant.parse("2026-11-09T08:00:00Z")),
        statistics);
  }
}
