package com.radiofind.account.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.event.AccountDeletedEvent;
import com.radiofind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

  private final UserRepository userRepository;
  private final TrackRepository trackRepository;
  private final PasswordConfirmation passwordConfirmation;
  private final ApplicationEventPublisher eventPublisher;
  private final TransactionTemplate transactionTemplate;
  private final TrackFileStorageService trackFileStorageService;
  private final FileStorageService fileStorageService;

  public void delete(CustomUserDetails userDetails, String password) {
    passwordConfirmation.require(userDetails.getUser(), password);

    RemovedAccount removed = transactionTemplate.execute(status -> remove(userDetails.getUser().getId()));

    removed.tracks().forEach(track -> {
      deleteFile(removed.userId(), track.getAudioFileName(), trackFileStorageService::deleteAudio);
      deleteFile(removed.userId(), track.getCoverFileName(), trackFileStorageService::deleteCover);
    });
    deleteFile(removed.userId(), removed.avatarFileName(), fileStorageService::deleteAvatar);
  }

  private RemovedAccount remove(long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    List<Track> tracks = trackRepository.findAllByOwnerId(userId);

    tracks.forEach(track -> {
      eventPublisher.publishEvent(TrackDeletedEvent.ofClosedAccount(track));
      trackRepository.delete(track);
    });
    eventPublisher.publishEvent(new AccountDeletedEvent(userId));
    userRepository.delete(user);

    return new RemovedAccount(userId, user.getAvatarFileName(), tracks);
  }

  private void deleteFile(long userId, String fileName, Consumer<String> deletion) {
    if (fileName == null) {
      return;
    }

    try {
      deletion.accept(fileName);
    } catch (IllegalStateException e) {
      log.atWarn()
          .setMessage("Deleted account left a file behind")
          .addKeyValue("userId", userId)
          .addKeyValue("fileName", fileName)
          .setCause(e)
          .log();
    }
  }

  private record RemovedAccount(long userId, String avatarFileName, List<Track> tracks) {
  }
}
