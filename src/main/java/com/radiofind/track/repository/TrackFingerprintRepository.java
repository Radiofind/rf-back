package com.radiofind.track.repository;

import com.radiofind.track.entity.TrackFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrackFingerprintRepository extends JpaRepository<TrackFingerprint, Long> {

  @Query("""
      select f from TrackFingerprint f join fetch f.track t
      where t.durationSeconds between :minDuration and :maxDuration
      """)
  List<TrackFingerprint> findWithDurationBetween(
      @Param("minDuration") int minDuration,
      @Param("maxDuration") int maxDuration);
}
