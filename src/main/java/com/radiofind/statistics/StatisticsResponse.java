package com.radiofind.statistics;

import com.radiofind.statistics.period.Granularity;
import com.radiofind.statistics.period.StatisticsPeriod;
import com.radiofind.track.dto.TrackSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;

public record StatisticsResponse(
    StatisticsPeriod period,
    Granularity granularity,
    Totals current,
    Totals previous,
    List<TimelinePoint> timeline,
    List<GenrePlays> genres,
    List<TopTrack> topTracks) {

  public record Totals(long plays, long downloads, long likes, long uniqueListeners) {
  }

  public record TimelinePoint(LocalDateTime start, long plays, long downloads) {
  }

  public record GenrePlays(String genre, long plays) {
  }

  public record TopTrack(TrackSummaryResponse track, long plays) {
  }
}
