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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private TransactionTemplate transactionTemplate;

  @Mock
  private TrackFileStorageService trackFileStorageService;

  @Mock
  private FileStorageService fileStorageService;

  @Test
  void tracksAreDeletedBeforeTheAccountItself() {
    AccountDeletionService service = new AccountDeletionService(userRepository, trackRepository,
        new PasswordConfirmation(passwordEncoder), eventPublisher, transactionTemplate,
        trackFileStorageService, fileStorageService);
    User user = User.builder().id(7L).password("hash").build();
    Track track = Track.builder().id(42L).owner(user).audioFileName("song.mp3").build();
    when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
    when(transactionTemplate.execute(any())).thenAnswer(
        invocation -> invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(trackRepository.findAllByOwnerId(7L)).thenReturn(List.of(track));

    service.delete(new CustomUserDetails(user), "secret");

    InOrder order = inOrder(eventPublisher, trackRepository, userRepository);
    order.verify(eventPublisher).publishEvent(TrackDeletedEvent.ofClosedAccount(track));
    order.verify(trackRepository).delete(track);
    order.verify(eventPublisher).publishEvent(new AccountDeletedEvent(7L));
    order.verify(userRepository).delete(user);
  }

  @Test
  void filesOfTheDeletedAccountAreRemoved() {
    AccountDeletionService service = new AccountDeletionService(userRepository, trackRepository,
        new PasswordConfirmation(passwordEncoder), eventPublisher, transactionTemplate,
        trackFileStorageService, fileStorageService);
    User user = User.builder().id(7L).password("hash").avatarFileName("avatar.png").build();
    Track track = Track.builder().id(42L).owner(user).audioFileName("song.mp3").coverFileName("cover.png").build();
    when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
    when(transactionTemplate.execute(any())).thenAnswer(
        invocation -> invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(trackRepository.findAllByOwnerId(7L)).thenReturn(List.of(track));

    service.delete(new CustomUserDetails(user), "secret");

    verify(trackFileStorageService).deleteAudio("song.mp3");
    verify(trackFileStorageService).deleteCover("cover.png");
    verify(fileStorageService).deleteAvatar("avatar.png");
  }

  @Test
  void accountIsKeptWhenThePasswordIsWrong() {
    AccountDeletionService service = new AccountDeletionService(userRepository, trackRepository,
        new PasswordConfirmation(passwordEncoder), eventPublisher, transactionTemplate,
        trackFileStorageService, fileStorageService);
    User user = User.builder().id(7L).password("hash").build();
    when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> service.delete(new CustomUserDetails(user), "wrong"));

    verifyNoInteractions(transactionTemplate, userRepository, trackRepository);
  }
}
