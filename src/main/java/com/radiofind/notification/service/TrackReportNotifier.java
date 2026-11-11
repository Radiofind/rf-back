package com.radiofind.notification.service;

import com.radiofind.notification.entity.Notification;
import com.radiofind.notification.entity.NotificationType;
import com.radiofind.notification.repository.NotificationRepository;
import com.radiofind.track.event.TrackReportedEvent;
import com.radiofind.user.entity.Role;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackReportNotifier {

  private final UserRepository userRepository;

  private final NotificationRepository notificationRepository;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void notifyAdmins(TrackReportedEvent event) {
    notificationRepository.saveAll(userRepository.findAllByRole(Role.ADMIN).stream()
        .map(admin -> Notification.builder()
            .recipient(admin)
            .type(NotificationType.TRACK_REPORTED)
            .track(event.track())
            .build())
        .toList());
  }
}
