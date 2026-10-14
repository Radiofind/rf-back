package com.radiofind.settings.repository;

import com.radiofind.settings.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
  Optional<UserSettings> findByUserId(Long userId);
}
