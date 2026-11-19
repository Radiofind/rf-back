package com.radiofind.statistics;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.statistics.StatisticsRepository.TrackPlays;
import com.radiofind.statistics.StatisticsResponse.TimelinePoint;
import com.radiofind.statistics.StatisticsResponse.TopTrack;
import com.radiofind.statistics.StatisticsResponse.Totals;
import com.radiofind.statistics.period.ReportingWindow;
import com.radiofind.statistics.period.StatisticsPeriod;
import com.radiofind.track.dto.TrackSummaryResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.TrackMapper;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

  private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");

  @Mock
  private StatisticsRepository statisticsRepository;

  @Mock
  private TrackRepository trackRepository;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @Spy
  private Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);

  @InjectMocks
  private StatisticsService statisticsService;

  @Test
  void timelineHasAPointForEveryDayEvenWithoutActivity() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    ReportingWindow window = new ReportingWindow(ZURICH, ChronoUnit.DAYS, LocalDate.parse("2026-11-13"), 7);
    when(statisticsRepository.playsPerBucket(7L, window)).thenReturn(Map.of(LocalDate.parse("2026-11-14"), 5L));
    when(statisticsRepository.downloadsPerBucket(7L, window)).thenReturn(Map.of(LocalDate.parse("2026-11-19"), 2L));

    StatisticsResponse statistics = statisticsService.getStatistics(ada, StatisticsPeriod.LAST_7_DAYS, ZURICH);

    assertEquals(
        List.of(
            new TimelinePoint(LocalDate.parse("2026-11-13"), 0, 0),
            new TimelinePoint(LocalDate.parse("2026-11-14"), 5, 0),
            new TimelinePoint(LocalDate.parse("2026-11-15"), 0, 0),
            new TimelinePoint(LocalDate.parse("2026-11-16"), 0, 0),
            new TimelinePoint(LocalDate.parse("2026-11-17"), 0, 0),
            new TimelinePoint(LocalDate.parse("2026-11-18"), 0, 0),
            new TimelinePoint(LocalDate.parse("2026-11-19"), 0, 2)),
        statistics.timeline());
  }

  @Test
  void periodIsComparedWithThePrecedingOneOfTheSameLength() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(statisticsRepository.totals(7L, Instant.parse("2026-11-12T23:00:00Z"), Instant.parse("2026-11-19T23:00:00Z")))
        .thenReturn(new Totals(120, 8, 14, 60));
    when(statisticsRepository.totals(7L, Instant.parse("2026-11-05T23:00:00Z"), Instant.parse("2026-11-12T23:00:00Z")))
        .thenReturn(new Totals(100, 10, 7, 61));

    StatisticsResponse statistics = statisticsService.getStatistics(ada, StatisticsPeriod.LAST_7_DAYS, ZURICH);

    assertEquals(new Totals(120, 8, 14, 60), statistics.current());
    assertEquals(new Totals(100, 10, 7, 61), statistics.previous());
  }

  @Test
  void topTracksKeepTheirRankAndShowTheTrack() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(statisticsRepository.mostPlayedTracks(anyLong(), any(), any(), anyInt()))
        .thenReturn(List.of(new TrackPlays(43L, 90), new TrackPlays(42L, 30)));
    when(trackRepository.findAllById(List.of(43L, 42L))).thenReturn(List.of(
        Track.builder().id(42L).title("Midnight Dreams").build(),
        Track.builder().id(43L).title("Neon Rain").build()));

    StatisticsResponse statistics = statisticsService.getStatistics(ada, StatisticsPeriod.LAST_30_DAYS, ZURICH);

    assertEquals(
        List.of(
            new TopTrack(TrackSummaryResponse.builder().id(43L).title("Neon Rain").build(), 90),
            new TopTrack(TrackSummaryResponse.builder().id(42L).title("Midnight Dreams").build(), 30)),
        statistics.topTracks());
  }

  @Test
  void fixedOffsetInsteadOfARegionIsRejected() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    ZoneId offset = ZoneOffset.ofHours(2);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> statisticsService.getStatistics(ada, StatisticsPeriod.LAST_7_DAYS, offset));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(statisticsRepository);
  }
}
