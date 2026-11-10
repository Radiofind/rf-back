package com.radiofind.track.repository;

import com.radiofind.track.entity.TrackLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

public interface TrackLikeRepository extends JpaRepository<TrackLike, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO track_likes (track_id, user_id, created_at)
      VALUES (:trackId, :userId, :createdAt)
      ON CONFLICT (track_id, user_id) DO NOTHING""", nativeQuery = true)
  int insertIfAbsent(
      @Param("trackId") long trackId,
      @Param("userId") long userId,
      @Param("createdAt") Instant createdAt);

  @Modifying
  @Query("DELETE FROM TrackLike l WHERE l.track.id = :trackId AND l.user.id = :userId")
  int deleteByTrackAndUser(@Param("trackId") long trackId, @Param("userId") long userId);

  @Query("SELECT l.track.id FROM TrackLike l WHERE l.user.id = :userId AND l.track.id IN :trackIds")
  Set<Long> findLikedTrackIds(@Param("userId") long userId, @Param("trackIds") Collection<Long> trackIds);
}
