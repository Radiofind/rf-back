package com.radiofind.track.repository;

import com.radiofind.track.entity.TrackListen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TrackListenRepository extends JpaRepository<TrackListen, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO track_listens (track_id, listener_id, created_at)
      VALUES (:trackId, :listenerId, :createdAt)
      ON CONFLICT (track_id, listener_id) DO NOTHING""", nativeQuery = true)
  int insertIfAbsent(
      @Param("trackId") long trackId,
      @Param("listenerId") long listenerId,
      @Param("createdAt") Instant createdAt);

  @Modifying
  @Query("DELETE FROM TrackListen l WHERE l.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);

  long countByTrackId(Long trackId);

  long countByTrackIdAndCreatedAtAfter(Long trackId, Instant since);

  @Query("SELECT MAX(l.createdAt) FROM TrackListen l WHERE l.track.id = :trackId")
  Optional<Instant> findLastListenedAt(@Param("trackId") long trackId);

  @Modifying
  @Query("DELETE FROM TrackListen l WHERE l.listener.id = :listenerId")
  int deleteByListenerId(@Param("listenerId") long listenerId);
}
