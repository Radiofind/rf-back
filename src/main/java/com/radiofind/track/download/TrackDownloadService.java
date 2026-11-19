package com.radiofind.track.download;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.event.AccountDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class TrackDownloadService {

  private final TrackRepository trackRepository;

  private final TrackDownloadRepository trackDownloadRepository;

  private final Clock clock;

  @Transactional
  public void recordDownload(CustomUserDetails userDetails, Long trackId) {
    Track track = trackRepository.findById(trackId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Track not found"));
    Long userId = userDetails.getUser().getId();

    if (track.getOwner().getId().equals(userId)) {
      return;
    }

    trackDownloadRepository.insert(trackId, userId, clock.instant());
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedTrack(TrackDeletedEvent event) {
    trackDownloadRepository.deleteByTrackId(event.trackId());
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void forgetDeletedAccount(AccountDeletedEvent event) {
    trackDownloadRepository.deleteByUserId(event.userId());
  }
}
