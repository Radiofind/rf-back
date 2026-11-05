package com.radiofind.notification.controller;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.notification.dto.NotificationResponse;
import com.radiofind.notification.dto.UnreadNotificationsResponse;
import com.radiofind.notification.service.NotificationService;
import com.radiofind.security.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public PageResponse<NotificationResponse> getNotifications(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
    return notificationService.getNotifications(userDetails, page, size);
  }

  @GetMapping("/unread-count")
  public UnreadNotificationsResponse countUnread(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return notificationService.countUnread(userDetails);
  }

  @PostMapping("/{notificationId}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAsRead(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long notificationId) {
    notificationService.markAsRead(userDetails, notificationId);
  }

  @PostMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
    notificationService.markAllAsRead(userDetails);
  }
}
