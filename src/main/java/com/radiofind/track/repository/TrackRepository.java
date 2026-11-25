package com.radiofind.track.repository;

import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long>, JpaSpecificationExecutor<Track> {

  long countByOwnerId(Long ownerId);

  long countByOwnerIdAndStatus(Long ownerId, TrackStatus status);

  @Query("SELECT COALESCE(SUM(t.likesCount), 0) FROM Track t WHERE t.owner.id = :ownerId")
  long sumLikesOfOwner(@Param("ownerId") Long ownerId);

  @Query("SELECT COALESCE(SUM(t.likesCount), 0) FROM Track t WHERE t.owner.id = :ownerId AND t.status = :status")
  long sumLikesOfOwnerInStatus(@Param("ownerId") long ownerId, @Param("status") TrackStatus status);

  List<Track> findAllByOwnerId(Long ownerId);

  Optional<Track> findByIdAndOwnerId(Long id, Long ownerId);

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

  @Modifying
  @Query("""
      UPDATE Track t SET t.likesCount = t.likesCount - 1
      WHERE t.likesCount > 0 AND t.id IN (SELECT l.track.id FROM TrackLike l WHERE l.user.id = :userId)""")
  int decrementLikesOfTracksLikedBy(@Param("userId") long userId);
}
