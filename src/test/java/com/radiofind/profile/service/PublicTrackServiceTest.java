package com.radiofind.profile.service;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.profile.access.ProfileAccess;
import com.radiofind.profile.access.ProfileAccessResolver;
import com.radiofind.profile.access.ProfileViewing;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.Track;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.track.service.TrackListing;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicTrackServiceTest {

  private static final UUID PUBLIC_ID = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");

  @Mock
  private PublicProfiles publicProfiles;

  @Mock
  private ProfileAccessResolver profileAccessResolver;

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private TrackListing trackListing;

  @InjectMocks
  private PublicTrackService publicTrackService;

  @Test
  void tracksOfARestrictedProfileAreForbidden() {
    User owner = User.builder().id(1L).build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(owner);
    when(profileAccessResolver.resolve(7L, owner))
        .thenReturn(new ProfileViewing(ProfileAccess.RESTRICTED, false, false));
    CustomUserDetails viewer = new CustomUserDetails(User.builder().id(7L).build());

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> publicTrackService.getTracks(viewer, PUBLIC_ID, 0, 10));

    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verifyNoInteractions(trackRepository);
  }

  @Test
  void tracksArePagedNewestFirstAndListedForTheViewer() {
    User owner = User.builder().id(1L).build();
    when(publicProfiles.require(PUBLIC_ID)).thenReturn(owner);
    when(profileAccessResolver.resolve(7L, owner))
        .thenReturn(new ProfileViewing(ProfileAccess.FULL, false, false));
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    Page<Track> tracks = new PageImpl<>(List.of());
    when(trackRepository.findAll(ArgumentMatchers.<Specification<Track>>any(), pageable.capture())).thenReturn(tracks);
    PageResponse<TrackResponse> listed = new PageResponse<>(List.of(), 2, 5, 0, false);
    when(trackListing.forListener(tracks, 7L)).thenReturn(listed);

    PageResponse<TrackResponse> result = publicTrackService.getTracks(
        new CustomUserDetails(User.builder().id(7L).build()), PUBLIC_ID, 2, 5);

    assertSame(listed, result);
    assertEquals(
        PageRequest.of(2, 5, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))),
        pageable.getValue());
  }
}
