package com.radiofind.notification.service;

import com.radiofind.notification.entity.Notification;
import com.radiofind.notification.entity.NotificationType;
import com.radiofind.notification.repository.NotificationRepository;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackReportedEvent;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackReportNotifierTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationRepository notificationRepository;

  @Captor
  private ArgumentCaptor<List<Notification>> notifications;

  @InjectMocks
  private TrackReportNotifier trackReportNotifier;

  @Test
  void everyAdminIsAlertedAboutTheReportedTrack() {
    User grace = User.builder().id(1L).build();
    User linus = User.builder().id(2L).build();
    Track track = Track.builder().id(42L).build();
    when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(List.of(grace, linus));

    trackReportNotifier.notifyAdmins(new TrackReportedEvent(track));

    verify(notificationRepository).saveAll(notifications.capture());
    assertEquals(
        List.of(grace, linus),
        notifications.getValue().stream().map(Notification::getRecipient).toList());
    assertEquals(
        List.of(NotificationType.TRACK_REPORTED, NotificationType.TRACK_REPORTED),
        notifications.getValue().stream().map(Notification::getType).toList());
  }
}
