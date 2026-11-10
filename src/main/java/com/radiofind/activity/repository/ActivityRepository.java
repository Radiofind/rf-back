package com.radiofind.activity.repository;

import com.radiofind.activity.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

  @EntityGraph(attributePaths = "track")
  Page<Activity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

  @Modifying
  @Query("DELETE FROM Activity a WHERE a.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);
}
