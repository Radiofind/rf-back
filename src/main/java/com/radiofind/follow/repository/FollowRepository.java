package com.radiofind.follow.repository;

import com.radiofind.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface FollowRepository extends JpaRepository<Follow, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO user_follows (follower_id, followee_id, created_at)
      VALUES (:followerId, :followeeId, :createdAt)
      ON CONFLICT (follower_id, followee_id) DO NOTHING""", nativeQuery = true)
  int insertIfAbsent(
      @Param("followerId") long followerId,
      @Param("followeeId") long followeeId,
      @Param("createdAt") Instant createdAt);

  @Modifying
  @Query("DELETE FROM Follow f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId")
  int deleteByPair(@Param("followerId") long followerId, @Param("followeeId") long followeeId);

  @Query("""
      SELECT COUNT(f) > 0 FROM Follow f
      WHERE f.follower.id = :followerId AND f.followee.id = :followeeId""")
  boolean existsByPair(@Param("followerId") long followerId, @Param("followeeId") long followeeId);

  @Query("SELECT COUNT(f) FROM Follow f WHERE f.followee.id = :followeeId")
  long countFollowers(@Param("followeeId") Long followeeId);

  @Modifying
  @Query("DELETE FROM Follow f WHERE f.follower.id = :userId OR f.followee.id = :userId")
  int deleteByUserId(@Param("userId") long userId);
}
