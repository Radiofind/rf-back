package com.radiofind.auth.repository;

import com.radiofind.auth.entity.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, Long> {

  Optional<TwoFactorChallenge> findByChallengeId(String challengeId);
}
