package com.radiofind.user.repository;

import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  List<User> findAllByRole(Role role);

  @Modifying
  @Query("""
      UPDATE User u SET u.totpLastUsedStep = :step
      WHERE u.id = :userId AND (u.totpLastUsedStep IS NULL OR u.totpLastUsedStep < :step)""")
  int claimTotpStep(@Param("userId") long userId, @Param("step") long step);
}
