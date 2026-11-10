package com.radiofind.track.editing;

import com.radiofind.genre.service.GenreCatalog;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.ImageType;
import com.radiofind.storage.service.TestImages;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.entity.IllegalTrackStatusException;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.OwnTracks;
import com.radiofind.track.service.TrackInputValidator;
import com.radiofind.track.service.TrackMapper;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackEditingServiceTest {

  private final GenreCatalog genreCatalog = mock(GenreCatalog.class);

  @Spy
  private TrackInputValidator trackInputValidator = new TrackInputValidator(genreCatalog);

  @Mock
  private OwnTracks ownTracks;

  @Mock
  private TrackFileStorageService trackFileStorageService;

  @Mock
  private TrackRepository trackRepository;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @Spy
  private TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

  @InjectMocks
  private TrackEditingService trackEditingService;

  @Test
  void editedTrackShowsItsNewDetailsAndKeepsItsStatus() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).name("Ada").surname("Lovelace").build());
    Track track = Track.builder()
        .id(42L)
        .owner(ada.getUser())
        .title("Old")
        .status(TrackStatus.REJECTED)
        .coverFileName("old.png")
        .build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    EditTrackRequest request = new EditTrackRequest();
    request.setTitle(" Midnight Dreams ");
    request.setGenres(List.of("GRUNGE"));
    request.setDescription("  ");

    TrackDetailsResponse details = trackEditingService.edit(ada, 42L, request, null);

    assertEquals("Midnight Dreams", details.title());
    assertEquals(List.of("GRUNGE"), details.genres());
    assertNull(details.description());
    assertEquals(TrackStatus.REJECTED, details.status());
    assertEquals("Ada Lovelace", details.uploadedBy());
    verify(trackFileStorageService, never()).deleteCover("old.png");
  }

  @Test
  void newCoverReplacesTheOldFileOnceSaved() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).owner(ada.getUser()).status(TrackStatus.DRAFT).coverFileName("old.png").build();
    MockMultipartFile cover = new MockMultipartFile("cover", "c.png", "image/png", TestImages.PNG);
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveCover(cover, ImageType.PNG)).thenReturn("new.png");
    EditTrackRequest request = new EditTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));

    trackEditingService.edit(ada, 42L, request, cover);

    assertEquals("new.png", track.getCoverFileName());
    verify(trackFileStorageService).deleteCover("old.png");
  }

  @Test
  void removedCoverIsDeleted() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).owner(ada.getUser()).status(TrackStatus.DRAFT).coverFileName("old.png").build();
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    EditTrackRequest request = new EditTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
    request.setRemoveCover(true);

    trackEditingService.edit(ada, 42L, request, null);

    assertNull(track.getCoverFileName());
    verify(trackFileStorageService).deleteCover("old.png");
  }

  @Test
  void publishedTrackIsNotEditedAndItsUploadedCoverIsDropped() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    Track track = Track.builder().id(42L).status(TrackStatus.APPROVED).coverFileName("old.png").build();
    MockMultipartFile cover = new MockMultipartFile("cover", "c.png", "image/png", TestImages.PNG);
    when(ownTracks.require(ada, 42L)).thenReturn(track);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveCover(any(), any())).thenReturn("new.png");
    EditTrackRequest request = new EditTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));

    assertThrows(IllegalTrackStatusException.class, () -> trackEditingService.edit(ada, 42L, request, cover));

    verify(trackFileStorageService).deleteCover("new.png");
    verify(trackFileStorageService, never()).deleteCover("old.png");
  }
}
