package com.radiofind.track.library;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackDetailsResponse;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.entity.TrackLike;
import com.radiofind.track.repository.TrackLikeRepository;
import com.radiofind.track.service.AudioStreamTokens;
import com.radiofind.track.service.TrackMapper;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

  @Mock
  private TrackLikeRepository trackLikeRepository;

  @Mock
  private AudioStreamTokens audioStreamTokens;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @InjectMocks
  private LibraryService libraryService;

  @Test
  void likedTracksArePlayableAndCarryTheMomentTheyWereAdded() {
    Track track = Track.builder().id(42L).owner(User.builder().id(1L).build()).build();
    TrackLike like = TrackLike.builder()
        .id(5L)
        .track(track)
        .user(User.builder().id(7L).build())
        .createdAt(Instant.parse("2026-11-01T09:00:00Z"))
        .build();
    when(trackLikeRepository.findAll(
        ArgumentMatchers.<Specification<TrackLike>>any(), ArgumentMatchers.any(Pageable.class)))
        .thenAnswer(invocation -> new PageImpl<>(List.of(like), invocation.getArgument(1), 1));
    when(audioStreamTokens.issue(42L)).thenReturn("signed");

    PageResponse<TrackResponse> library = libraryService.getLibrary(
        new CustomUserDetails(User.builder().id(7L).build()),
        new LibraryQuery(null, LibrarySort.ADDED, Sort.Direction.DESC, 0, 10));

    TrackResponse item = library.items().getFirst();
    assertTrue(item.isLiked());
    assertEquals(Instant.parse("2026-11-01T09:00:00Z"), item.getLikedAt());
    assertEquals("/api/tracks/42/audio?token=signed", item.getAudioUrl());
    assertFalse(item.isOwn());
  }

  @Test
  void ownTrackInTheLibraryIsMarkedAsOwn() {
    User ada = User.builder().id(7L).build();
    TrackLike like = TrackLike.builder()
        .id(5L)
        .track(Track.builder().id(42L).owner(ada).build())
        .user(ada)
        .build();
    when(trackLikeRepository.findAll(
        ArgumentMatchers.<Specification<TrackLike>>any(), ArgumentMatchers.any(Pageable.class)))
        .thenAnswer(invocation -> new PageImpl<>(List.of(like), invocation.getArgument(1), 1));

    PageResponse<TrackResponse> library = libraryService.getLibrary(
        new CustomUserDetails(ada),
        new LibraryQuery(null, LibrarySort.ADDED, Sort.Direction.DESC, 0, 10));

    assertTrue(library.items().getFirst().isOwn());
  }

  @Test
  void libraryIsPagedAndSortedAsRequested() {
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    when(trackLikeRepository.findAll(ArgumentMatchers.<Specification<TrackLike>>any(), pageable.capture()))
        .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

    libraryService.getLibrary(
        new CustomUserDetails(User.builder().id(7L).build()),
        new LibraryQuery("neon", LibrarySort.PLAYS, Sort.Direction.ASC, 2, 20));

    assertEquals(2, pageable.getValue().getPageNumber());
    assertEquals(20, pageable.getValue().getPageSize());
    assertEquals(
        Sort.by(new Sort.Order(Sort.Direction.ASC, "track.playsCount").nullsLast(), Sort.Order.desc("id")),
        pageable.getValue().getSort());
  }

  @Test
  void detailsOfALibraryTrackDoNotRevealTheUploadersRealName() {
    Track track = Track.builder()
        .id(42L)
        .title("Midnight Dreams")
        .artistName("Neon Lights")
        .owner(User.builder().id(1L).name("Ada").surname("Lovelace").build())
        .build();
    when(trackLikeRepository.findLikedTrack(42L, 7L)).thenReturn(Optional.of(track));

    TrackDetailsResponse details = libraryService.getDetails(
        new CustomUserDetails(User.builder().id(7L).build()), 42L);

    assertEquals("Neon Lights", details.artistName());
    assertNull(details.uploadedBy());
  }

  @Test
  void detailsOfATrackOutsideTheLibraryAreNotFound() {
    when(trackLikeRepository.findLikedTrack(42L, 7L)).thenReturn(Optional.empty());

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> libraryService.getDetails(
        new CustomUserDetails(User.builder().id(7L).build()), 42L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    verify(trackLikeRepository).findLikedTrack(42L, 7L);
  }
}
