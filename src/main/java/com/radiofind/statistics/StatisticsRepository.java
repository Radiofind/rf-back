package com.radiofind.statistics;

import com.radiofind.statistics.StatisticsResponse.GenrePlays;
import com.radiofind.statistics.StatisticsResponse.Totals;
import com.radiofind.statistics.period.ReportingWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StatisticsRepository {

  private static final String TOTALS = """
      SELECT
        (SELECT count(*) FROM track_plays p JOIN tracks t ON t.id = p.track_id
          WHERE t.owner_id = :ownerId AND p.played_at >= :from AND p.played_at < :to) AS plays,
        (SELECT count(*) FROM track_downloads d JOIN tracks t ON t.id = d.track_id
          WHERE t.owner_id = :ownerId AND d.downloaded_at >= :from AND d.downloaded_at < :to) AS downloads,
        (SELECT count(*) FROM track_likes l JOIN tracks t ON t.id = l.track_id
          WHERE t.owner_id = :ownerId AND l.created_at >= :from AND l.created_at < :to) AS likes,
        (SELECT count(DISTINCT p.listener_id) FROM track_plays p JOIN tracks t ON t.id = p.track_id
          WHERE t.owner_id = :ownerId AND p.played_at >= :from AND p.played_at < :to) AS unique_listeners""";

  private static final String PLAYS_PER_BUCKET = """
      SELECT CAST(date_trunc(:unit, p.played_at AT TIME ZONE :zone) AS date) AS bucket, count(*) AS total
      FROM track_plays p JOIN tracks t ON t.id = p.track_id
      WHERE t.owner_id = :ownerId AND p.played_at >= :from AND p.played_at < :to
      GROUP BY bucket""";

  private static final String DOWNLOADS_PER_BUCKET = """
      SELECT CAST(date_trunc(:unit, d.downloaded_at AT TIME ZONE :zone) AS date) AS bucket, count(*) AS total
      FROM track_downloads d JOIN tracks t ON t.id = d.track_id
      WHERE t.owner_id = :ownerId AND d.downloaded_at >= :from AND d.downloaded_at < :to
      GROUP BY bucket""";

  private static final String PLAYS_PER_GENRE = """
      SELECT g.genre, count(*) AS plays
      FROM track_plays p
        JOIN tracks t ON t.id = p.track_id
        JOIN track_genres g ON g.track_id = t.id AND g.position = 0
      WHERE t.owner_id = :ownerId AND p.played_at >= :from AND p.played_at < :to
      GROUP BY g.genre
      ORDER BY plays DESC, g.genre""";

  private static final String MOST_PLAYED_TRACKS = """
      SELECT p.track_id, count(*) AS plays
      FROM track_plays p JOIN tracks t ON t.id = p.track_id
      WHERE t.owner_id = :ownerId AND p.played_at >= :from AND p.played_at < :to
      GROUP BY p.track_id
      ORDER BY plays DESC, p.track_id
      LIMIT :limit""";

  private final JdbcClient jdbcClient;

  public Totals totals(long ownerId, Instant from, Instant to) {
    return jdbcClient.sql(TOTALS)
        .param("ownerId", ownerId)
        .param("from", timestamp(from))
        .param("to", timestamp(to))
        .query((rs, row) -> new Totals(
            rs.getLong("plays"),
            rs.getLong("downloads"),
            rs.getLong("likes"),
            rs.getLong("unique_listeners")))
        .single();
  }

  public Map<LocalDate, Long> playsPerBucket(long ownerId, ReportingWindow window) {
    return perBucket(PLAYS_PER_BUCKET, ownerId, window);
  }

  public Map<LocalDate, Long> downloadsPerBucket(long ownerId, ReportingWindow window) {
    return perBucket(DOWNLOADS_PER_BUCKET, ownerId, window);
  }

  public List<GenrePlays> playsPerGenre(long ownerId, Instant from, Instant to) {
    return jdbcClient.sql(PLAYS_PER_GENRE)
        .param("ownerId", ownerId)
        .param("from", timestamp(from))
        .param("to", timestamp(to))
        .query((rs, row) -> new GenrePlays(rs.getString("genre"), rs.getLong("plays")))
        .list();
  }

  public List<TrackPlays> mostPlayedTracks(long ownerId, Instant from, Instant to, int limit) {
    return jdbcClient.sql(MOST_PLAYED_TRACKS)
        .param("ownerId", ownerId)
        .param("from", timestamp(from))
        .param("to", timestamp(to))
        .param("limit", limit)
        .query((rs, row) -> new TrackPlays(rs.getLong("track_id"), rs.getLong("plays")))
        .list();
  }

  private Map<LocalDate, Long> perBucket(String sql, long ownerId, ReportingWindow window) {
    return jdbcClient.sql(sql)
        .param("unit", window.unit() == ChronoUnit.MONTHS ? "month" : "day")
        .param("zone", window.zone().getId())
        .param("ownerId", ownerId)
        .param("from", timestamp(window.from()))
        .param("to", timestamp(window.to()))
        .query(StatisticsRepository::bucketCount)
        .list()
        .stream()
        .collect(Collectors.toMap(BucketCount::bucket, BucketCount::total));
  }

  private static BucketCount bucketCount(ResultSet rs, int row) throws SQLException {
    return new BucketCount(rs.getObject("bucket", LocalDate.class), rs.getLong("total"));
  }

  private static OffsetDateTime timestamp(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  public record TrackPlays(long trackId, long plays) {
  }

  private record BucketCount(LocalDate bucket, long total) {
  }
}
