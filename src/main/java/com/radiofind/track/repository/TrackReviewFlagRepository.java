package com.radiofind.track.repository;

import com.radiofind.track.entity.TrackReviewFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackReviewFlagRepository extends JpaRepository<TrackReviewFlag, Long> {

  @Modifying
  @Query("DELETE FROM TrackReviewFlag f WHERE f.track.id = :trackId OR f.matchedTrack.id = :trackId")
  int deleteInvolving(@Param("trackId") long trackId);
}
