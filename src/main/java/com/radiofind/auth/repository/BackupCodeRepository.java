package com.radiofind.auth.repository;

import com.radiofind.auth.entity.BackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {

  List<BackupCode> findByUserIdAndUsedAtIsNull(Long userId);

  long countByUserIdAndUsedAtIsNull(Long userId);

  @Modifying
  @Query("DELETE FROM BackupCode c WHERE c.user.id = :userId")
  int deleteByUserId(@Param("userId") long userId);
}
