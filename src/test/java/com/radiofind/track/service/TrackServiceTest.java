package com.radiofind.track.service;

import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.common.dto.PageResponse;
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
import com.radiofind.track.entity.TrackReviewFlag;
import com.radiofind.track.entity.TrackReviewFlagType;
import com.radiofind.track.fingerprint.AudioFingerprint;
import com.radiofind.track.fingerprint.AudioFingerprinter;
import com.radiofind.track.fingerprint.DuplicateCheck;
import com.radiofind.track.fingerprint.DuplicateTrackDetector;
import com.radiofind.track.fingerprint.TrackMatch;
import com.radiofind.track.fingerprint.UnreadableAudioException;
import com.radiofind.track.repository.TrackFingerprintRepository;
import com.radiofind.track.repository.TrackReviewFlagRepository;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

  private static final byte[] MP3 = "ID3\u0004\0\0\0\0\0\0".getBytes(StandardCharsets.ISO_8859_1);

  private static final AudioFingerprint FINGERPRINT = new AudioFingerprint(214, new int[] { 1, 2, 3 });

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

  @Mock
  private AudioFingerprinter audioFingerprinter;

  @Mock
  private DuplicateTrackDetector duplicateTrackDetector;

  @Mock
  private TrackFingerprintRepository trackFingerprintRepository;

  @Mock
  private TrackReviewFlagRepository trackReviewFlagRepository;

  @Mock
  private AudioStreamTokens audioStreamTokens;

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
    request.setGenres(List.of(" BLUES_ROCK ", "GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("BLUES_ROCK")).thenReturn(true);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveAudio(audio, AudioType.MP3)).thenReturn("a.mp3");
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(audioFingerprinter.fingerprint(any())).thenReturn(FINGERPRINT);
    when(duplicateTrackDetector.check(7L, FINGERPRINT)).thenReturn(new DuplicateCheck(Optional.empty(), List.of()));
    when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    ArgumentCaptor<TrackUploadedEvent> event = ArgumentCaptor.forClass(TrackUploadedEvent.class);

    TrackResponse response = trackService.upload(new CustomUserDetails(owner), request, audio, null);

    verify(eventPublisher).publishEvent(event.capture());
    Track track = event.getValue().track();
    assertEquals(owner, track.getOwner());
    assertEquals("Midnight Dreams", track.getTitle());
    assertEquals(List.of("BLUES_ROCK", "GRUNGE"), track.getGenres());
    assertEquals(TrackStatus.PENDING_REVIEW, track.getStatus());
    assertEquals("a.mp3", track.getAudioFileName());
    assertEquals(214, track.getDurationSeconds());
    assertEquals("The Engines", response.getArtistName());
  }

  @Test
  void artistNameFallsBackToFullNameWhenProfileHasNoArtistName() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(audioFingerprinter.fingerprint(any())).thenReturn(FINGERPRINT);
    when(duplicateTrackDetector.check(7L, FINGERPRINT)).thenReturn(new DuplicateCheck(Optional.empty(), List.of()));
    when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TrackResponse response = trackService.upload(new CustomUserDetails(owner), request, audio, null);

    assertEquals("Ada Lovelace", response.getArtistName());
  }

  @Test
  void coverIsStoredInItsDetectedFormatAndExposedAsUrl() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    MockMultipartFile cover = new MockMultipartFile("cover", "cover.jpg", "image/jpeg", TestImages.PNG);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveCover(cover, ImageType.PNG)).thenReturn("c.png");
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(audioFingerprinter.fingerprint(any())).thenReturn(FINGERPRINT);
    when(duplicateTrackDetector.check(7L, FINGERPRINT)).thenReturn(new DuplicateCheck(Optional.empty(), List.of()));
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
    request.setGenres(List.of("POLKA_STEP"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, null));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(trackFileStorageService);
  }

  @Test
  void rejectsTheSameGenreTwice() {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE", "GRUNGE "));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, null));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verifyNoInteractions(trackFileStorageService);
  }

  @Test
  void rejectsAudioWhoseContentIsNotMp3WavOrFlac() {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
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
    request.setGenres(List.of("GRUNGE"));
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
    request.setGenres(List.of("GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    MockMultipartFile cover = new MockMultipartFile("cover", "cover.png", "image/png", TestImages.PNG);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveAudio(audio, AudioType.MP3)).thenReturn("a.mp3");
    when(trackFileStorageService.saveCover(cover, ImageType.PNG)).thenReturn("c.png");
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(audioFingerprinter.fingerprint(any())).thenReturn(FINGERPRINT);
    when(duplicateTrackDetector.check(7L, FINGERPRINT)).thenReturn(new DuplicateCheck(Optional.empty(), List.of()));
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
  void ownUploadsAreListedNewestFirstWithGenresStatusAndUploadTime() {
    User owner = User.builder().id(7L).build();
    Track track = Track.builder()
        .id(42L)
        .title("Midnight Dreams")
        .genres(List.of("AMBIENT"))
        .status(TrackStatus.PENDING_REVIEW)
        .createdAt(Instant.parse("2025-04-28T14:32:00Z"))
        .build();
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    when(trackRepository.findAll(ArgumentMatchers.<Specification<Track>>any(), pageable.capture()))
        .thenAnswer(invocation -> new PageImpl<>(List.of(track), invocation.getArgument(1), 8));
    when(audioStreamTokens.issue(42L)).thenReturn("signed");

    PageResponse<TrackResponse> uploads = trackService.getOwnTracks(new CustomUserDetails(owner), null, null, 1, 7);

    assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")), pageable.getValue().getSort());
    assertEquals(1, pageable.getValue().getPageNumber());
    assertEquals(8, uploads.totalElements());
    TrackResponse upload = uploads.items().getFirst();
    assertEquals(List.of("AMBIENT"), upload.getGenres());
    assertEquals(TrackStatus.PENDING_REVIEW, upload.getStatus());
    assertEquals(Instant.parse("2025-04-28T14:32:00Z"), upload.getCreatedAt());
    assertEquals("/api/tracks/42/audio?token=signed", upload.getAudioUrl());
  }

  @Test
  void audioIsServedInItsFormatToTheHolderOfItsToken() {
    Track track = Track.builder().id(42L).audioFileName("a.flac").audioFormat(AudioType.FLAC).build();
    when(audioStreamTokens.grants("signed", 42L)).thenReturn(true);
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));
    when(trackFileStorageService.getAudioPath("a.flac")).thenReturn(Path.of("/audio/a.flac"));

    TrackAudio audio = trackService.getAudio(42L, "signed");

    assertEquals(new TrackAudio(Path.of("/audio/a.flac"), AudioType.FLAC.getMediaType()), audio);
  }

  @Test
  void audioIsForbiddenWithoutAValidToken() {
    when(audioStreamTokens.grants("forged", 42L)).thenReturn(false);

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class, () -> trackService.getAudio(42L, "forged"));

    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verifyNoInteractions(trackRepository);
  }

  @Test
  void trackWithoutCoverHasNoCoverToServe() {
    Track track = Track.builder().id(42L).build();
    when(trackRepository.findById(42L)).thenReturn(Optional.of(track));

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> trackService.getCover(42L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }

  @Test
  void rejectsATrackTheUserHasAlreadyUploadedAndDropsItsFiles() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    Track earlierUpload = Track.builder().id(41L).owner(owner).status(TrackStatus.APPROVED).build();
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveAudio(audio, AudioType.MP3)).thenReturn("a.mp3");
    when(audioFingerprinter.fingerprint(any())).thenReturn(FINGERPRINT);
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(duplicateTrackDetector.check(7L, FINGERPRINT))
        .thenReturn(new DuplicateCheck(Optional.of(earlierUpload), List.of()));

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(owner), request, audio, null));

    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(trackRepository, never()).save(any());
    verify(trackFileStorageService).deleteAudio("a.mp3");
  }

  @Test
  void flagsEveryResemblingTrackForTheReviewer() {
    User owner = User.builder().id(7L).name("Ada").surname("Lovelace").build();
    User otherArtist = User.builder().id(8L).build();
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    Track rejected = Track.builder().id(40L).owner(owner).status(TrackStatus.REJECTED).build();
    Track foreign = Track.builder().id(41L).owner(otherArtist).status(TrackStatus.APPROVED).build();
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(audioFingerprinter.fingerprint(any())).thenReturn(FINGERPRINT);
    when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
    when(duplicateTrackDetector.check(7L, FINGERPRINT)).thenReturn(new DuplicateCheck(Optional.empty(), List.of(
        new TrackMatch(rejected, TrackReviewFlagType.REUPLOAD_OF_REJECTED_TRACK, 0.97),
        new TrackMatch(foreign, TrackReviewFlagType.MATCHES_OTHER_USERS_TRACK, 0.91))));
    when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    ArgumentCaptor<List<TrackReviewFlag>> flags = ArgumentCaptor.captor();

    trackService.upload(new CustomUserDetails(owner), request, audio, null);

    verify(trackReviewFlagRepository).saveAll(flags.capture());
    assertEquals(List.of(TrackReviewFlagType.REUPLOAD_OF_REJECTED_TRACK, TrackReviewFlagType.MATCHES_OTHER_USERS_TRACK),
        flags.getValue().stream().map(TrackReviewFlag::getType).toList());
    assertEquals(List.of(rejected, foreign),
        flags.getValue().stream().map(TrackReviewFlag::getMatchedTrack).toList());
  }

  @Test
  void rejectsAudioThatCannotBeDecodedAndDropsItsFile() {
    UploadTrackRequest request = new UploadTrackRequest();
    request.setTitle("Midnight Dreams");
    request.setGenres(List.of("GRUNGE"));
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", MP3);
    when(genreCatalog.isGenre("GRUNGE")).thenReturn(true);
    when(trackFileStorageService.saveAudio(audio, AudioType.MP3)).thenReturn("a.mp3");
    when(audioFingerprinter.fingerprint(any())).thenThrow(new UnreadableAudioException("Audio file could not be decoded"));

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> trackService.upload(new CustomUserDetails(User.builder().id(7L).build()), request, audio, null));

    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, error.getStatusCode());
    verify(trackFileStorageService).deleteAudio("a.mp3");
    verifyNoInteractions(trackRepository);
  }
}
