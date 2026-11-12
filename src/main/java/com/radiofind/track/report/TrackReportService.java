package com.radiofind.track.report;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.user.event.AccountDeletedEvent;
import com.radiofind.track.event.TrackReportedEvent;
import com.radiofind.track.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class TrackReportService {

  private final TrackRepository trackRepository;

  private final TrackReportRepository trackReportRepository;

  private final ApplicationEventPublisher eventPublisher;

  private final Clock clock;

  @Transactional
  public void report(CustomUserDetails userDetails, Long trackId, TrackReportRequest request) {
    Long reporterId = userDetails.getUser().getId();
    Track track = trackRepository.findById(trackId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));

    if (track.getOwner().getId().equals(reporterId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot report your own track");
    }

    track.requireStatus(TrackStatus.APPROVED, "reported");
    String comment = normalize(request.comment());

    if (request.reason() == TrackReportReason.OTHER && comment == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describe the problem when the reason is OTHER");
    }

    if (trackReportRepository.insertIfAbsent(
        trackId, reporterId, request.reason().name(), comment, clock.instant()) == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this track");
    }

    eventPublisher.publishEvent(new TrackReportedEvent(track));
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedTrack(TrackDeletedEvent event) {
    trackReportRepository.deleteByTrackId(event.trackId());
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedAccount(AccountDeletedEvent event) {
    trackReportRepository.deleteByReporterId(event.userId());
  }

  private String normalize(String comment) {
    return comment == null || comment.isBlank() ? null : comment.trim();
  }
}
