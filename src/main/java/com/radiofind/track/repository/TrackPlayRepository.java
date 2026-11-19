package com.radiofind.track.repository;

import com.radiofind.track.entity.TrackPlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TrackPlayRepository extends JpaRepository<TrackPlay, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO track_plays (track_id, listener_id, played_at)
      VALUES (:trackId, :listenerId, :playedAt)""", nativeQuery = true)
  int insert(
      @Param("trackId") long trackId,
      @Param("listenerId") long listenerId,
      @Param("playedAt") Instant playedAt);

  long countByTrackId(Long trackId);

  @Modifying
  @Query("DELETE FROM TrackPlay p WHERE p.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);

  @Modifying
  @Query("DELETE FROM TrackPlay p WHERE p.listener.id = :listenerId")
  int deleteByListenerId(@Param("listenerId") long listenerId);
}
