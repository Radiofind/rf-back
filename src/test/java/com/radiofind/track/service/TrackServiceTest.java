package com.radiofind.track.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.genre.service.GenreCatalog;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.AudioType;
import com.radiofind.storage.service.ImageType;
import com.radiofind.storage.service.TestImages;
import com.radiofind.storage.service.TrackFileStorageService;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.UploadTrackRequest;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.event.TrackUploadedEvent;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

  private static final byte[] MP3 = "ID3\u0004\0\0\0\0\0\0".getBytes(StandardCharsets.ISO_8859_1);

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TrackFileStorageService trackFileStorageService;

  @Mock
  private GenreCatalog genreCatalog;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @Spy
  private TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

  @InjectMocks
  private TrackService trackService;

  @Test
  void uploadedTrackIsPendingReviewAndAnnouncedUnderTheArtistName() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    owner.setArtistProfile(ArtistProfile.builder().artistName(" The Engines ").build());
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle(" Midnight Dreams ");
    request.setGenre("BLUES_ROCK");
    request.setSubgenre("GRUNGE");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("BLUES_ROCK")).thenReturn(true);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveAudio(audio, AudioType.MP3)).thenReturn("a.mp3");
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    ArgumentCaptor<TrackUploadedEvent> event = ArgumentCaptor.forClass(TrackUploadedEvent.class);

    TrackResponse response = trackService.upload(new CustomUserDetails(owner), request, audio, null);

    verify(eventPublisher).publishEvent(event.capture());
    Track track = event.getValue().track();
    assertEquals(owner, track.getOwner());
    assertEquals("Midnight Dreams", track.getTitle());
    assertEquals("GRUNGE", track.getSubgenre());
    assertEquals(TrackStatus.PENDING_REVIEW, track.getStatus());
    assertEquals("a.mp3", track.getAudioFileName());
    assertEquals("The Engines", response.getArtistName());
  }

  @Test
  void artistNameFallsBackToFullNameWhenProfileHasNoArtistName() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre("GRUNGE");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TrackResponse response = trackService.upload(new CustomUserDetails(owner), request, audio, null);

    assertEquals("Ada Lovelace", response.getArtistName());
  }

  @Test
  void coverIsStoredInItsDetectedFormatAndExposedAsUrl() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre("GRUNGE");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", TestImages.PNG);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveCover(cover, ImageType.PNG)).thenReturn("c.png");
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(trackRepository.save(any())).thenAnswer(invocation -> {
      Track track = invocation.getArgument(0);
      track.setId(42L);
      return track;
    });

    TrackResponse response = trackService.upload(new CustomUserDetails(owner), request, audio, cover);

    assertEquals("/api/tracks/42/cover", response.getCoverUrl());
  }

  @Test
  void rejectsUnknownGenreBeforeStoringAnyFile() {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre("POLKA_STEP");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, null));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(trackFileStorageService);
  }

  @ParameterizedTest
  @CsvSource({
      "BLUES_ROCK, ROCK",
      "BLUES_ROCK, BLUES_ROCK"
  })
  void rejectsSubgenreThatIsAGroupOrRepeatsTheGenre(String genre, String subgenre) {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre(genre);
    request.setSubgenre(subgenre);
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("BLUES_ROCK")).thenReturn(true);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, null));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(trackFileStorageService);
  }

  @Test
  void rejectsAudioWhoseContentIsNotMp3WavOrFlac() {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre("GRUNGE");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", TestImages.HTML);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, null));

    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, error.getStatusCode());
    verifyNoInteractions(trackFileStorageService);
  }

  @Test
  void rejectsCoverWhoseContentIsNotAnImage() {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre("GRUNGE");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    MockMultipartFile cover = new MockMultipartFile("cover", "cover.png", "image/png", TestImages.HTML);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, cover));

    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, error.getStatusCode());
    verifyNoInteractions(trackFileStorageService);
  }

  @Test
  void storedFilesAreDeletedWhenTheTrackCannotBeSaved() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenre("GRUNGE");
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    MockMultipartFile cover = new MockMultipartFile("cover", "cover.png", "image/png", TestImages.PNG);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveAudio(audio, AudioType.MP3)).thenReturn("a.mp3");
    when(trackFileStorageService.saveCover(cover, ImageType.PNG)).thenReturn("c.png");
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(trackRepository.save(any())).thenThrow(new DataIntegrityViolationException("constraint"));

    assertThrows(DataIntegrityViolationException.class,
        () -> trackService.upload(new CustomUserDetails(owner), request, audio, cover));

    verify(trackFileStorageService).deleteAudio("a.mp3");
    verify(trackFileStorageService).deleteCover("c.png");
  }

  @Test
  void coverOfAnyTrackIsServedInItsImageFormat() {
    Track track = Track.builder().id(42L).coverFileName("c.png").build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackFileStorageService.coverExists("c.png")).thenReturn(true);
    when(trackFileStorageService.getCoverPath("c.png")).thenReturn(Path.of("/covers/c.png"));

    TrackCover cover = trackService.getCover(42L);

    assertEquals(new TrackCover(Path.of("/covers/c.png"), MediaType.IMAGE_PNG), cover);
  }

  @Test
  void trackWithoutCoverHasNoCoverToServe() {
    Track track = Track.builder().id(42L).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> trackService.getCover(42L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }
}
