package com.radiofind.auth.repository;

import com.radiofind.auth.entity.PasswordResetToken;
import com.radiofind.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  void deleteByUserAndUsedFalse(User user);

  @Modifying
  @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
  int deleteByUserId(@Param("userId") long userId);
}
