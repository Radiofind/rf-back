package com.radiofind.auth.repository;

import com.radiofind.auth.entity.TwoFactorChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TwoFactorChallenge> findWithLockByChallengeId(String challengeId);
}
