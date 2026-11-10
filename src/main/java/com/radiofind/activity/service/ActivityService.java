package com.radiofind.activity.service;

import com.radiofind.activity.dto.ActivityResponse;
import com.radiofind.activity.entity.Activity;
import com.radiofind.activity.entity.ActivityType;
import com.radiofind.activity.repository.ActivityRepository;
import com.radiofind.common.dto.PageResponse;
import com.radiofind.security.CustomUserDetails;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {

  private final ActivityRepository activityRepository;

  private final TrackMapper trackMapper;

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordTrackUploaded(TrackUploadedEvent event) {
    record(event.track(), ActivityType.TRACK_UPLOADED);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordSubmissionCancelled(TrackSubmissionCancelledEvent event) {
    record(event.track(), ActivityType.TRACK_SUBMISSION_CANCELLED);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordTrackResubmitted(TrackResubmittedEvent event) {
    record(event.track(), ActivityType.TRACK_RESUBMITTED);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordScheduleChangeRequested(ScheduleChangeRequestedEvent event) {
    record(event.track(), ActivityType.SCHEDULE_CHANGE_REQUESTED);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedTrack(TrackDeletedEvent event) {
    activityRepository.deleteByTrackId(event.trackId());
  }

  @Transactional(readOnly = true)
  public PageResponse<ActivityResponse> getActivities(CustomUserDetails userDetails, int page, int size) {
    return PageResponse.of(
        activityRepository.findByUserIdOrderByCreatedAtDescIdDesc(
            userDetails.getUser().getId(),
            PageRequest.of(page, size)),
        this::toResponse);
  }

  private void record(Track track, ActivityType type) {
    activityRepository.save(Activity.builder()
        .user(track.getOwner())
        .type(type)
        .track(track)
        .build());
  }

  private ActivityResponse toResponse(Activity activity) {
    return ActivityResponse.builder()
        .id(activity.getId())
        .type(activity.getType())
        .createdAt(activity.getCreatedAt())
        .track(activity.getTrack() != null ? trackMapper.toSummary(activity.getTrack()) : null)
        .build();
  }
}
