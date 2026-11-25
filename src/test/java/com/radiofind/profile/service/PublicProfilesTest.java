package com.radiofind.profile.service;

import com.radiofind.user.repository.UserRepository;
import com.radiofind.user.service.DeletedAccounts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProfilesTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private DeletedAccounts deletedAccounts;

  @InjectMocks
  private PublicProfiles publicProfiles;

  @Test
  void profileIsGoneWhenItsAccountWasDeleted() {
    UUID publicId = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.empty());
    when(deletedAccounts.isRemembered(publicId)).thenReturn(true);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> publicProfiles.require(publicId));

    assertEquals(HttpStatus.GONE, error.getStatusCode());
  }

  @Test
  void profileIsNotFoundWhenNoRecentlyDeletedAccountHadItsId() {
    UUID publicId = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");
    when(userRepository.findByPublicId(publicId)).thenReturn(Optional.empty());
    when(deletedAccounts.isRemembered(publicId)).thenReturn(false);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> publicProfiles.require(publicId));

    assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  }
}
