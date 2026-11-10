package com.radiofind.notification.service;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.notification.dto.NotificationResponse;
import com.radiofind.notification.dto.UnreadNotificationsResponse;
import com.radiofind.notification.entity.Notification;
import com.radiofind.notification.entity.NotificationType;
import com.radiofind.notification.repository.NotificationRepository;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackSummaryResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.ScheduleChangeRequestedEvent;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.event.TrackResubmittedEvent;
import com.radiofind.track.event.TrackSubmissionCancelledEvent;
import com.radiofind.track.event.TrackUploadedEvent;
import com.radiofind.track.service.TrackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;

  private final TrackMapper trackMapper;

  private final Clock clock;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void notifyTrackSubmittedForReview(TrackUploadedEvent event) {
    notify(event.track(), NotificationType.TRACK_SUBMITTED_FOR_REVIEW);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void notifySubmissionCancelled(TrackSubmissionCancelledEvent event) {
    notify(event.track(), NotificationType.TRACK_SUBMISSION_CANCELLED);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void notifyTrackResubmitted(TrackResubmittedEvent event) {
    notify(event.track(), NotificationType.TRACK_RESUBMITTED_FOR_REVIEW);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void notifyScheduleChangeRequested(ScheduleChangeRequestedEvent event) {
    notify(event.track(), NotificationType.SCHEDULE_CHANGE_REQUESTED);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void notifyTrackDeleted(TrackDeletedEvent event) {
    notificationRepository.deleteByTrackId(event.trackId());
    notificationRepository.save(Notification.builder()
        .recipient(event.owner())
        .type(NotificationType.TRACK_DELETED)
        .trackTitle(event.title())
        .trackArtistName(event.artistName())
        .build());
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationResponse> getNotifications(CustomUserDetails userDetails, int page, int size) {
    return PageResponse.of(
        notificationRepository.findByRecipientIdOrderByCreatedAtDescIdDesc(
            userDetails.getUser().getId(),
            PageRequest.of(page, size)),
        this::toResponse);
  }

  @Transactional(readOnly = true)
  public UnreadNotificationsResponse countUnread(CustomUserDetails userDetails) {
    return new UnreadNotificationsResponse(
        notificationRepository.countByRecipientIdAndReadAtIsNull(userDetails.getUser().getId()));
  }

  @Transactional
  public void markAsRead(CustomUserDetails userDetails, Long notificationId) {
    Notification notification = notificationRepository
        .findByIdAndRecipientId(notificationId, userDetails.getUser().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now(clock));
    }
  }

  @Transactional
  public void markAllAsRead(CustomUserDetails userDetails) {
    notificationRepository.markAllAsRead(userDetails.getUser().getId(), Instant.now(clock));
  }

  private void notify(Track track, NotificationType type) {
    notificationRepository.save(Notification.builder()
        .recipient(track.getOwner())
        .type(type)
        .track(track)
        .build());
  }

  private NotificationResponse toResponse(Notification notification) {
    return NotificationResponse.builder()
        .id(notification.getId())
        .type(notification.getType())
        .read(notification.getReadAt() != null)
        .createdAt(notification.getCreatedAt())
        .track(trackOf(notification))
        .build();
  }

  private TrackSummaryResponse trackOf(Notification notification) {
    if (notification.getTrack() != null) {
      return trackMapper.toSummary(notification.getTrack());
    }

    return notification.getTrackTitle() != null
        ? trackMapper.toDeletedSummary(notification.getTrackTitle(), notification.getTrackArtistName())
        : null;
  }
}
