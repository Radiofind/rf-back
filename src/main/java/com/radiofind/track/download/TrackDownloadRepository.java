package com.radiofind.track.download;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TrackDownloadRepository extends JpaRepository<TrackDownload, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO track_downloads (track_id, user_id, downloaded_at)
      VALUES (:trackId, :userId, :downloadedAt)""", nativeQuery = true)
  int insert(
      @Param("trackId") long trackId,
      @Param("userId") long userId,
      @Param("downloadedAt") Instant downloadedAt);

  @Modifying
  @Query("DELETE FROM TrackDownload d WHERE d.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);

  @Modifying
  @Query("DELETE FROM TrackDownload d WHERE d.user.id = :userId")
  int deleteByUserId(@Param("userId") long userId);
}
