package com.radiofind.notification.repository;

import com.radiofind.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  @EntityGraph(attributePaths = "track")
  Page<Notification> findByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId, Pageable pageable);

  long countByRecipientIdAndReadAtIsNull(Long recipientId);

  Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

  @Modifying
  @Query("update Notification n set n.readAt = :readAt where n.recipient.id = :recipientId and n.readAt is null")
  int markAllAsRead(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);

  @Modifying
  @Query("DELETE FROM Notification n WHERE n.track.id = :trackId")
  int deleteByTrackId(@Param("trackId") long trackId);
}
