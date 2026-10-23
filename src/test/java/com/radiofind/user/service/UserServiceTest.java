package com.radiofind.user.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

  private static final String AVATAR_URL = "/api/users/me/avatar";

  @TempDir
  Path avatarsPath;

  private UserRepository userRepository;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    userService = new UserService(
        userRepository,
        new FileStorageService(avatarsPath.toString()));
  }

  private CustomUserDetails userWithAvatar(String avatarFileName) {
    User user = User.builder()
        .name("Ada")
        .surname("Lovelace")
        .email("ada@example.com")
        .avatarFileName(avatarFileName)
        .build();

    when(userRepository.findById(any())).thenReturn(Optional.of(user));

    return new CustomUserDetails(user);
  }

  @Test
  void currentUserHasAvatarUrlWhenFileIsStored() throws IOException {
    Files.writeString(avatarsPath.resolve("avatar.png"), "image");

    assertEquals(AVATAR_URL, userService.getCurrentUser(userWithAvatar("avatar.png")).getAvatarUrl());
  }

  @Test
  void currentUserHasNoAvatarUrlWhenFileIsMissing() {
    assertNull(userService.getCurrentUser(userWithAvatar("lost.png")).getAvatarUrl());
  }

  @Test
  void currentUserHasNoAvatarUrlWithoutAvatar() {
    assertNull(userService.getCurrentUser(userWithAvatar(null)).getAvatarUrl());
  }

  @Test
  void profileHasAvatarUrlWhenFileIsStored() throws IOException {
    Files.writeString(avatarsPath.resolve("avatar.png"), "image");

    assertEquals(AVATAR_URL, userService.getUserProfile(userWithAvatar("avatar.png")).getAvatarUrl());
  }

  @Test
  void profileHasNoAvatarUrlWhenFileIsMissing() {
    assertNull(userService.getUserProfile(userWithAvatar("lost.png")).getAvatarUrl());
  }
}
