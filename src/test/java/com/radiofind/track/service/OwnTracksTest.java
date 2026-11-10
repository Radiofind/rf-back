package com.radiofind.track.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.track.repository.TrackRepository;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnTracksTest {

  @Mock
  private TrackRepository trackRepository;

  @InjectMocks
  private OwnTracks ownTracks;

  @Test
  void trackOfAnotherUserIsNotFound() {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).build());
    when(trackRepository.findByIdAndOwnerId(42L, 7L)).thenReturn(Optional.empty());

    ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> ownTracks.require(ada, 42L));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }
}
