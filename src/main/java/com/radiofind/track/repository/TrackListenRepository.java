package com.radiofind.track.repository;

import com.radiofind.track.entity.TrackListen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

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
}
