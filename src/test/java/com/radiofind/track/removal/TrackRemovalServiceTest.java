package com.radiofind.track.removal;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.entity.Track;
import com.radiofind.track.event.TrackDeletedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackRemovalServiceTest {

  @Mock
  private OwnTracks ownTracks;

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private TrackFileStorageService trackFileStorageService;

  @Spy
  private TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

  @InjectMocks
  private TrackRemovalService trackRemovalService;

  @Test
  void deletedTrackTakesItsDataAndFilesAlong() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder()
        .id(42L)
        .owner(ada.getUser())
        .title("Midnight Dreams")
        .artistName("Neon Lights")
        .audioFileName("a.wav")
        .coverFileName("c.png")
        .build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);

    trackRemovalService.delete(ada, 42L);

    InOrder order = inOrder(eventPublisher, trackRepository, trackFileStorageService);
    order.verify(eventPublisher)
        .publishEvent(new TrackDeletedEvent(42L, ada.getUser(), "Midnight Dreams", "Neon Lights", false));
    order.verify(trackRepository).delete(track);
    order.verify(trackFileStorageService).deleteAudio("a.wav");
    verify(trackFileStorageService).deleteCover("c.png");
  }

  @Test
  void fileThatCannotBeDeletedDoesNotFailTheDeletion() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).audioFileName("a.wav").coverFileName("c.png").build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    doThrow(new IllegalStateException("disk")).when(trackFileStorageService).deleteAudio("a.wav");

    assertDoesNotThrow(() -> trackRemovalService.delete(ada, 42L));

    verify(trackFileStorageService).deleteCover("c.png");
  }
}
