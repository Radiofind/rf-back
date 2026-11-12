package com.radiofind.settings.repository;

import com.radiofind.settings.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
  Optional<UserSettings> findByUserId(Long userId);

  @Modifying
  @Query("DELETE FROM UserSettings s WHERE s.user.id = :userId")
  int deleteByUserId(@Param("userId") long userId);
}
