package com.radiofind.track.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TrackReportRepository extends JpaRepository<TrackReport, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO track_reports (track_id, reporter_id, reason, comment, status, created_at)
      VALUES (:trackId, :reporterId, :reason, CAST(:comment AS VARCHAR), 'OPEN', :createdAt)
      ON CONFLICT (track_id, reporter_id) DO NOTHING""", nativeQuery = true)
  int insertIfAbsent(
      @Param("trackId") long trackId,
      @Param("reporterId") long reporterId,
      @Param("reason") String reason,
      @Param("comment") String comment,
      @Param("createdAt") Instant createdAt);

  @Modifying
  @Query("DELETE FROM TrackReport r WHERE r.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);
}
