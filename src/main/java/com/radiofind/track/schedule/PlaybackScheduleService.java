package com.radiofind.track.schedule;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.ScheduleChangeRequestedEvent;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.service.OwnTracks;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlaybackScheduleService {

  private final OwnTracks ownTracks;

  private final PlaybackScheduler playbackScheduler;

  private final ScheduleChangeRequestRepository scheduleChangeRequestRepository;

  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public PlaybackScheduleResponse getSchedule(CustomUserDetails userDetails, Long trackId) {
    Track track = scheduledTrack(userDetails, trackId);

    return new PlaybackScheduleResponse(
        playbackScheduler.nextAiring(track),
        track.getDurationSeconds(),
        scheduleChangeRequestRepository
            .findFirstByTrackIdAndStatus(trackId, ScheduleChangeRequestStatus.PENDING)
            .map(PendingScheduleChange::of)
            .orElse(null));
  }

  @Transactional
  public PendingScheduleChange requestChange(
      CustomUserDetails userDetails,
      Long trackId,
      ScheduleChangeRequestBody body) {
    Track track = scheduledTrack(userDetails, trackId);

    if (scheduleChangeRequestRepository
        .findFirstByTrackIdAndStatus(trackId, ScheduleChangeRequestStatus.PENDING)
        .isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A schedule change is already under review");
    }

    ScheduleChangeRequest request = scheduleChangeRequestRepository.saveAndFlush(ScheduleChangeRequest.builder()
        .track(track)
        .preferredAt(body.preferredAt())
        .comment(body.comment() == null || body.comment().isBlank() ? null : body.comment().trim())
        .status(ScheduleChangeRequestStatus.PENDING)
        .build());
    eventPublisher.publishEvent(new ScheduleChangeRequestedEvent(track));

    return PendingScheduleChange.of(request);
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedTrack(TrackDeletedEvent event) {
    scheduleChangeRequestRepository.deleteByTrackId(event.trackId());
  }

  private Track scheduledTrack(CustomUserDetails userDetails, Long trackId) {
    Track track = ownTracks.require(userDetails, trackId);
    track.requireStatus(TrackStatus.APPROVED, "scheduled");

    return track;
  }
}
