package com.radiofind.track.repository;

import com.radiofind.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackRepository extends JpaRepository<Track, Long>, JpaSpecificationExecutor<Track> {

  long countByOwnerId(Long ownerId);

  @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:ownerId)", nativeQuery = true)
  int lockUploadsOf(@Param("ownerId") long ownerId);

  @Modifying
  @Query("UPDATE Track t SET t.playsCount = t.playsCount + 1 WHERE t.id = :trackId")
  int incrementPlays(@Param("trackId") long trackId);

  @Modifying
  @Query("UPDATE Track t SET t.likesCount = t.likesCount + 1 WHERE t.id = :trackId")
  int incrementLikes(@Param("trackId") long trackId);

  @Modifying
  @Query("UPDATE Track t SET t.likesCount = t.likesCount - 1 WHERE t.id = :trackId AND t.likesCount > 0")
  int decrementLikes(@Param("trackId") long trackId);
}
