package com.radiofind.statistics;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.statistics.StatisticsRepository.TrackPlays;
import com.radiofind.statistics.StatisticsResponse.TimelinePoint;
import com.radiofind.statistics.StatisticsResponse.TopTrack;
import com.radiofind.statistics.period.ReportingWindow;
import com.radiofind.statistics.period.StatisticsPeriod;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.TrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

  private static final int TOP_TRACKS = 5;

  private static final ZoneId UTC = ZoneId.of("UTC");

  private final StatisticsRepository statisticsRepository;

  private final TrackRepository trackRepository;

  private final TrackMapper trackMapper;

  private final Clock clock;

  @Transactional(readOnly = true)
  public StatisticsResponse getStatistics(CustomUserDetails userDetails, StatisticsPeriod period, ZoneId zone) {
    ReportingWindow window = period.windowAt(clock.instant(), regionOf(zone));
    long ownerId = userDetails.getUser().getId();

    return new StatisticsResponse(
        period,
        window.granularity(),
        statisticsRepository.totals(ownerId, window.from(), window.to()),
        statisticsRepository.totals(ownerId, window.previousFrom(), window.from()),
        timeline(ownerId, window),
        statisticsRepository.playsPerGenre(ownerId, window.from(), window.to()),
        topTracks(ownerId, window));
  }

  private List<TimelinePoint> timeline(long ownerId, ReportingWindow window) {
    Map<LocalDateTime, Long> plays = statisticsRepository.playsPerBucket(ownerId, window);
    Map<LocalDateTime, Long> downloads = statisticsRepository.downloadsPerBucket(ownerId, window);

    return window.bucketStarts().stream()
        .map(start -> new TimelinePoint(start, plays.getOrDefault(start, 0L), downloads.getOrDefault(start, 0L)))
        .toList();
  }

  private List<TopTrack> topTracks(long ownerId, ReportingWindow window) {
    List<TrackPlays> ranking = statisticsRepository.mostPlayedTracks(ownerId, window.from(), window.to(), TOP_TRACKS);
    Map<Long, Track> tracks = trackRepository.findAllById(ranking.stream().map(TrackPlays::trackId).toList())
        .stream()
        .collect(Collectors.toMap(Track::getId, Function.identity()));

    return ranking.stream()
        .filter(entry -> tracks.containsKey(entry.trackId()))
        .map(entry -> new TopTrack(trackMapper.toSummary(tracks.get(entry.trackId())), entry.plays()))
        .toList();
  }

  private static ZoneId regionOf(ZoneId zone) {
    if (!(zone instanceof ZoneOffset offset)) {
      return zone;
    }

    if (offset.equals(ZoneOffset.UTC)) {
      return UTC;
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time zone must be a region such as Europe/Zurich");
  }
}
