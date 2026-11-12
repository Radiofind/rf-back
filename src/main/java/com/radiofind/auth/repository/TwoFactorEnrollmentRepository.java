package com.radiofind.auth.repository;

import com.radiofind.auth.entity.TwoFactorEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TwoFactorEnrollmentRepository extends JpaRepository<TwoFactorEnrollment, Long> {

  Optional<TwoFactorEnrollment> findByUserId(Long userId);

  @Modifying
  @Query("DELETE FROM TwoFactorEnrollment e WHERE e.user.id = :userId")
  int deleteByUserId(@Param("userId") long userId);
}
