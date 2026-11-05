package com.radiofind.notification.service;

import com.radiofind.notification.entity.Notification;
import com.radiofind.notification.entity.NotificationType;
import com.radiofind.notification.repository.NotificationRepository;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackUploadedEvent;
import com.radiofind.track.service.TrackMapper;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

  @Mock
  private NotificationRepository notificationRepository;

  @Test
  void ownerIsToldTheUploadedTrackWentToReview() {
    NotificationService service = new NotificationService(
        notificationRepository, new TrackMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    User owner = User.builder().id(7L).build();
    Track track = Track.builder().id(42L).owner(owner).build();
    ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);

    service.notifyTrackSubmittedForReview(new TrackUploadedEvent(track));

    verify(notificationRepository).save(notification.capture());
    assertSame(owner, notification.getValue().getRecipient());
    assertSame(track, notification.getValue().getTrack());
    assertEquals(NotificationType.TRACK_SUBMITTED_FOR_REVIEW, notification.getValue().getType());
  }

  @Test
  void notificationOfAnotherUserCannotBeMarkedAsRead() {
    NotificationService service = new NotificationService(
        notificationRepository, new TrackMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    when(notificationRepository.findByIdAndRecipientId(5L, 7L)).thenReturn(Optional.empty());

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.markAsRead(new CustomUserDetails(User.builder().id(7L).build()), 5L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }

  @Test
  void markingAsReadAgainKeepsTheFirstReadTime() {
    NotificationService service = new NotificationService(
        notificationRepository, new TrackMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    Instant firstRead = Instant.parse("2026-11-01T08:00:00Z");
    Notification notification = Notification.builder().id(5L).readAt(firstRead).build();
    when(notificationRepository.findByIdAndRecipientId(5L, 7L)).thenReturn(Optional.of(notification));

    service.markAsRead(new CustomUserDetails(User.builder().id(7L).build()), 5L);

    assertEquals(firstRead, notification.getReadAt());
  }

  @Test
  void unreadNotificationIsMarkedReadNow() {
    NotificationService service = new NotificationService(
        notificationRepository, new TrackMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    Notification notification = Notification.builder().id(5L).build();
    when(notificationRepository.findByIdAndRecipientId(5L, 7L)).thenReturn(Optional.of(notification));

    service.markAsRead(new CustomUserDetails(User.builder().id(7L).build()), 5L);

    assertEquals(NOW, notification.getReadAt());
  }
}
