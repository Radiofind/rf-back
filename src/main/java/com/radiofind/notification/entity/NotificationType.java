package com.radiofind.notification.entity;

public enum NotificationType {
  TRACK_SUBMITTED_FOR_REVIEW(NotificationCategory.TRACK_UPDATES),
  TRACK_SUBMISSION_CANCELLED(NotificationCategory.TRACK_UPDATES),
  TRACK_RESUBMITTED_FOR_REVIEW(NotificationCategory.TRACK_UPDATES),
  SCHEDULE_CHANGE_REQUESTED(NotificationCategory.RADIO_UPDATES),
  TRACK_DELETED(NotificationCategory.TRACK_UPDATES),
  TRACK_REPORTED(NotificationCategory.MODERATION),
  TRACK_LIKED(NotificationCategory.LIKES);

  private final NotificationCategory category;

  NotificationType(NotificationCategory category) {
    this.category = category;
  }

  public NotificationCategory category() {
    return category;
  }
}
