package com.radiofind.track.service;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackLikeRepository;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackListingTest {

  @Mock
  private TrackLikeRepository trackLikeRepository;

  @Spy
  private TrackMapper trackMapper = new TrackMapper();

  @Mock
  private AudioStreamTokens audioStreamTokens;

  @InjectMocks
  private TrackListing trackListing;

  @Test
  void listenerSeesWhichTracksTheyLikedAndWhoPublishedThem() {
    UUID artistPublicId = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");
    User artist = User.builder().id(1L).publicId(artistPublicId).build();
    Track liked = Track.builder().id(41L).owner(artist).genres(List.of()).build();
    Track notLiked = Track.builder().id(42L).owner(artist).genres(List.of()).build();
    when(trackLikeRepository.findLikedTrackIds(7L, List.of(41L, 42L))).thenReturn(Set.of(41L));
    when(audioStreamTokens.issue(41L)).thenReturn("token-41");
    when(audioStreamTokens.issue(42L)).thenReturn("token-42");

    List<TrackResponse> tracks = trackListing.forListener(new PageImpl<>(List.of(liked, notLiked)), 7L).items();

    assertTrue(tracks.get(0).isLiked());
    assertFalse(tracks.get(1).isLiked());
    assertEquals(artistPublicId, tracks.get(0).getArtistPublicId());
  }

  @Test
  void listenerOwnsOnlyTheirOwnTracks() {
    Track own = Track.builder().id(41L).owner(User.builder().id(7L).build()).genres(List.of()).build();
    Track foreign = Track.builder().id(42L).owner(User.builder().id(1L).build()).genres(List.of()).build();
    when(trackLikeRepository.findLikedTrackIds(7L, List.of(41L, 42L))).thenReturn(Set.of());

    List<TrackResponse> tracks = trackListing.forListener(new PageImpl<>(List.of(own, foreign)), 7L).items();

    assertTrue(tracks.get(0).isOwn());
    assertFalse(tracks.get(1).isOwn());
  }

  @Test
  void emptyPageNeedsNoLikeLookup() {
    PageResponse<TrackResponse> tracks = trackListing.forListener(new PageImpl<>(List.<Track>of()), 7L);

    assertTrue(tracks.items().isEmpty());
    verifyNoInteractions(trackLikeRepository);
  }
}
