package com.radiofind.user.service;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.entity.ArtistSocialLinks;
import com.radiofind.artist.entity.ArtistType;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.storage.service.TestImages;
import com.radiofind.user.dto.UpdateUserProfileRequest;
import com.radiofind.user.dto.UpdateUserProfileResponse;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

  private static final String AVATAR_URL = "/api/users/me/avatar";

  private static final long USER_ID = 7L;

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

  private User persistedUser(ArtistProfile artistProfile) {
    User user = User.builder()
        .id(USER_ID)
        .name("Ada")
        .surname("Lovelace")
        .email("ada@example.com")
        .artistProfile(artistProfile)
        .build();

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    return user;
  }

  private UpdateUserProfileRequest profileRequest(String email) {
    UpdateUserProfileRequest request = new UpdateUserProfileRequest();
    request.setName(" Ada ");
    request.setSurname(" Lovelace ");
    request.setEmail(email);
    return request;
  }

  private UpdateUserProfileResponse update(User user, UpdateUserProfileRequest request) {
    return userService.updateUserProfile(new CustomUserDetails(user), request);
  }

  @Test
  void updateProfileNormalizesEmailAndRequiresReauthenticationWhenEmailChanges() {
    User user = persistedUser(null);

    UpdateUserProfileResponse response = update(user, profileRequest("  Ada.King@Example.COM "));

    assertEquals("ada.king@example.com", user.getEmail());
    assertEquals("Ada", user.getName());
    assertEquals("Lovelace", user.getSurname());
    assertTrue(response.isEmailChanged());
    assertTrue(response.isRequiresReauthentication());
    verify(userRepository).existsByEmailIgnoreCaseAndIdNot("ada.king@example.com", USER_ID);
    verify(userRepository).save(user);
  }

  @Test
  void updateProfileDoesNotRequireReauthenticationWhenOnlyEmailCaseChanges() {
    User user = persistedUser(null);

    UpdateUserProfileResponse response = update(user, profileRequest("ADA@example.com"));

    assertEquals("ada@example.com", user.getEmail());
    assertFalse(response.isEmailChanged());
    assertFalse(response.isRequiresReauthentication());
  }

  @Test
  void updateProfileRejectsEmailUsedByAnotherUser() {
    User user = persistedUser(null);
    when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", USER_ID)).thenReturn(true);

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> update(user, profileRequest("Taken@Example.com")));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("ada@example.com", user.getEmail());
    verify(userRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "ftp://example.com/artist",
      "javascript:alert(1)",
      "example.com/artist",
      "https:///artist",
      "https://exa mple.com"
  })
  void updateProfileRejectsInvalidSocialLink(String url) {
    User user = persistedUser(null);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setSocialLinks(ArtistSocialLinksDto.builder().instagram(url).build());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> update(user, request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(userRepository, never()).save(any());
  }

  @Test
  void updateProfileStoresTrimmedSocialLinks() {
    User user = persistedUser(null);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setSocialLinks(ArtistSocialLinksDto.builder()
        .instagram(" https://instagram.com/ada ")
        .spotify("HTTP://open.spotify.com/artist/1")
        .youtube("  ")
        .build());

    update(user, request);

    ArtistSocialLinks socialLinks = user.getArtistProfile().getSocialLinks();
    assertEquals("https://instagram.com/ada", socialLinks.getInstagram());
    assertEquals("HTTP://open.spotify.com/artist/1", socialLinks.getSpotify());
    assertNull(socialLinks.getYoutube());
  }

  @Test
  void updateProfileStoresNullSocialLinksWhenAllLinksAreEmpty() {
    ArtistProfile artistProfile = ArtistProfile.builder()
        .socialLinks(ArtistSocialLinks.builder().instagram("https://instagram.com/ada").build())
        .build();
    User user = persistedUser(artistProfile);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setArtistName("Ada");
    request.setSocialLinks(ArtistSocialLinksDto.builder().instagram("").telegram("   ").build());

    UpdateUserProfileResponse response = update(user, request);

    assertNull(artistProfile.getSocialLinks());
    assertNull(response.getProfile().getSocialLinks());
  }

  @Test
  void updateProfileDropsBlankGenresAndCaseInsensitiveDuplicates() {
    User user = persistedUser(null);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setMainGenres(Arrays.asList(" Rock ", "", "rock", null, "Jazz", "  ", "JAZZ"));
    request.setAdditionalGenres(List.of("Ambient", "ambient "));

    UpdateUserProfileResponse response = update(user, request);

    assertEquals(List.of("Rock", "Jazz"), user.getArtistProfile().getMainGenres());
    assertEquals(List.of("Ambient"), user.getArtistProfile().getAdditionalGenres());
    assertEquals(List.of("Rock", "Jazz"), response.getProfile().getMainGenres());
  }

  @Test
  void updateProfileCreatesArtistProfileWithDefaultTypeWithoutArtistData() {
    User user = persistedUser(null);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setArtistName("   ");
    request.setMainGenres(List.of(" ", ""));
    request.setSocialLinks(ArtistSocialLinksDto.builder().tiktok("").build());

    UpdateUserProfileResponse response = update(user, request);

    ArtistProfile artistProfile = user.getArtistProfile();
    assertNotNull(artistProfile);
    assertSame(user, artistProfile.getUser());
    assertEquals(ArtistType.ARTIST, artistProfile.getType());
    assertNull(artistProfile.getArtistName());
    assertEquals(List.of(), artistProfile.getMainGenres());
    assertEquals(ArtistType.ARTIST, response.getProfile().getArtistType());
  }

  @Test
  void updateProfileKeepsCurrentArtistTypeWhenRequestHasNone() {
    ArtistProfile artistProfile = ArtistProfile.builder().type(ArtistType.BAND).build();
    User user = persistedUser(artistProfile);

    update(user, profileRequest("ada@example.com"));

    assertEquals(ArtistType.BAND, artistProfile.getType());
  }

  @Test
  void profileReportsDefaultArtistTypeForUserWithoutArtistProfile() {
    assertEquals(ArtistType.ARTIST, userService.getUserProfile(userWithAvatar(null)).getArtistType());
  }

  @Test
  void updateProfileCreatesArtistProfileWhenRequestContainsArtistData() {
    User user = persistedUser(null);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setArtistType(ArtistType.BAND);
    request.setArtistName(" The Engines ");

    UpdateUserProfileResponse response = update(user, request);

    ArtistProfile artistProfile = user.getArtistProfile();
    assertNotNull(artistProfile);
    assertSame(user, artistProfile.getUser());
    assertEquals(ArtistType.BAND, artistProfile.getType());
    assertEquals("The Engines", artistProfile.getArtistName());
    assertEquals("The Engines", response.getProfile().getArtistName());
  }

  @Test
  void updateProfileUpdatesExistingArtistProfile() {
    ArtistProfile artistProfile = ArtistProfile.builder()
        .id(3L)
        .type(ArtistType.ARTIST)
        .artistName("Old name")
        .shortDescription("Old short")
        .build();
    User user = persistedUser(artistProfile);
    UpdateUserProfileRequest request = profileRequest("ada@example.com");
    request.setArtistType(ArtistType.BAND);
    request.setArtistName("New name");
    request.setShortDescription(" ");
    request.setDescription(" Analytical engine band ");

    update(user, request);

    assertSame(artistProfile, user.getArtistProfile());
    assertEquals(3L, artistProfile.getId());
    assertEquals(ArtistType.BAND, artistProfile.getType());
    assertEquals("New name", artistProfile.getArtistName());
    assertNull(artistProfile.getShortDescription());
    assertEquals("Analytical engine band", artistProfile.getDescription());
  }

  private MockMultipartFile avatarFile(String fileName, String contentType, byte[] content) {
    return new MockMultipartFile("file", fileName, contentType, content);
  }

  private void assertAvatarRejected(MockMultipartFile file) throws IOException {
    CustomUserDetails userDetails = userWithAvatar(null);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.uploadAvatar(userDetails, file));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verify(userRepository, never()).save(any());
    try (var files = Files.list(avatarsPath)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void uploadAvatarRejectsEmptyFile() throws IOException {
    assertAvatarRejected(avatarFile("avatar.png", "image/png", new byte[0]));
  }

  @Test
  void uploadAvatarRejectsFileLargerThanFiveMegabytes() throws IOException {
    byte[] content = Arrays.copyOf(TestImages.PNG, 5 * 1024 * 1024 + 1);

    assertAvatarRejected(avatarFile("avatar.png", "image/png", content));
  }

  @Test
  void uploadAvatarRejectsUnsupportedContentType() throws IOException {
    assertAvatarRejected(avatarFile("avatar.svg", "image/svg+xml", TestImages.PNG));
  }

  @Test
  void uploadAvatarRejectsContentThatIsNotAnImage() throws IOException {
    assertAvatarRejected(avatarFile("avatar.png", "image/png", TestImages.HTML));
  }

  @Test
  void uploadAvatarStoresFileWithExtensionDetectedFromContent() {
    CustomUserDetails userDetails = userWithAvatar(null);
    User user = userDetails.getUser();

    assertEquals(AVATAR_URL,
        userService.uploadAvatar(userDetails, avatarFile("evil.html", "image/png", TestImages.PNG)).getAvatarUrl());

    assertTrue(user.getAvatarFileName().endsWith(".png"));
    assertTrue(Files.isRegularFile(avatarsPath.resolve(user.getAvatarFileName())));
    verify(userRepository).save(user);
  }

  @Test
  void uploadAvatarReplacesPreviousAvatar() throws IOException {
    Files.writeString(avatarsPath.resolve("old.png"), "image");
    CustomUserDetails userDetails = userWithAvatar("old.png");

    userService.uploadAvatar(userDetails, avatarFile("new.webp", "image/webp", TestImages.WEBP));

    assertFalse(Files.exists(avatarsPath.resolve("old.png")));
    assertTrue(userDetails.getUser().getAvatarFileName().endsWith(".webp"));
  }

  @Test
  void uploadAvatarRemovesNewFileWhenUserCannotBeSaved() throws IOException {
    Files.writeString(avatarsPath.resolve("old.png"), "image");
    CustomUserDetails userDetails = userWithAvatar("old.png");
    doThrow(new IllegalStateException("db down")).when(userRepository).save(any());

    assertThrows(IllegalStateException.class,
        () -> userService.uploadAvatar(userDetails, avatarFile("new.jpg", "image/jpeg", TestImages.JPEG)));

    assertEquals("old.png", userDetails.getUser().getAvatarFileName());
    try (var files = Files.list(avatarsPath)) {
      assertEquals(List.of(avatarsPath.resolve("old.png")), files.toList());
    }
  }

  @Test
  void deleteAvatarRemovesFileAndClearsFileName() throws IOException {
    Files.writeString(avatarsPath.resolve("avatar.png"), "image");
    CustomUserDetails userDetails = userWithAvatar("avatar.png");

    userService.deleteAvatar(userDetails);

    assertFalse(Files.exists(avatarsPath.resolve("avatar.png")));
    assertNull(userDetails.getUser().getAvatarFileName());
    verify(userRepository).save(userDetails.getUser());
  }

  @Test
  void deleteAvatarDoesNothingWithoutAvatar() {
    userService.deleteAvatar(userWithAvatar(null));

    verify(userRepository, never()).save(any());
  }
}
