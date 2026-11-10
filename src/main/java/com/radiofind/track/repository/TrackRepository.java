package com.radiofind.track.repository;

import com.radiofind.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackRepository extends JpaRepository<Track, Long>, JpaSpecificationExecutor<Track> {

  long countByOwnerId(Long ownerId);

  @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:ownerId)", nativeQuery = true)
  int lockUploadsOf(@Param("ownerId") long ownerId);
}
