package com.radiofind.track.removal;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.OwnTracks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackRemovalService {

  private final OwnTracks ownTracks;

  private final TrackRepository trackRepository;

  private final ApplicationEventPublisher eventPublisher;

  private final TrackFileStorageService trackFileStorageService;

  private final TransactionTemplate transactionTemplate;

  public void delete(CustomUserDetails userDetails, Long trackId) {
    Track track = transactionTemplate.execute(status -> remove(userDetails, trackId));

    deleteFile(track, track.getAudioFileName(), trackFileStorageService::deleteAudio);
    deleteFile(track, track.getCoverFileName(), trackFileStorageService::deleteCover);
  }

  private Track remove(CustomUserDetails userDetails, Long trackId) {
    Track track = ownTracks.require(userDetails, trackId);

    eventPublisher.publishEvent(TrackDeletedEvent.of(track));
    trackRepository.delete(track);

    return track;
  }

  private void deleteFile(Track track, String fileName, Consumer<String> deletion) {
    try {
      deletion.accept(fileName);
    } catch (IllegalStateException e) {
      log.atWarn()
          .setMessage("Deleted track left a file behind")
          .addKeyValue("trackId", track.getId())
          .addKeyValue("fileName", fileName)
          .setCause(e)
          .log();
    }
  }
}
