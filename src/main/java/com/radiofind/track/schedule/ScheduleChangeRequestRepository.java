package com.radiofind.track.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScheduleChangeRequestRepository extends JpaRepository<ScheduleChangeRequest, Long> {

  Optional<ScheduleChangeRequest> findFirstByTrackIdAndStatus(Long trackId, ScheduleChangeRequestStatus status);

  @Modifying
  @Query("DELETE FROM ScheduleChangeRequest r WHERE r.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);
}
