package com.radiofind.activity.repository;

import com.radiofind.activity.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

  @EntityGraph(attributePaths = "track")
  Page<Activity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
