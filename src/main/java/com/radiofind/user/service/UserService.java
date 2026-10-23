package com.radiofind.user.service;

import com.radiofind.artist.dto.ArtistSocialLinksDto;
import com.radiofind.artist.entity.ArtistProfile;
import com.radiofind.artist.entity.ArtistSocialLinks;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.user.dto.AvatarResponse;
import com.radiofind.user.dto.CurrentUserResponse;
import com.radiofind.user.dto.UpdateUserProfileRequest;
import com.radiofind.user.dto.UpdateUserProfileResponse;
import com.radiofind.user.dto.UserProfileResponse;
import com.radiofind.user.entity.User;
import com.radiofind.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

  private static final String AVATAR_URL = "/api/users/me/avatar";

  private final UserRepository userRepository;

  private final FileStorageService fileStorageService;

  public UserService(
      UserRepository userRepository,
      FileStorageService fileStorageService) {
    this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
  }

  public CurrentUserResponse getCurrentUser(
      CustomUserDetails userDetails) {

    User user = userDetails.getUser();

    return CurrentUserResponse.builder()
        .name(user.getName())
        .surname(user.getSurname())
        .email(user.getEmail())
        .avatarUrl(getAvatarUrl(user))
        .build();
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getUserProfile(
      CustomUserDetails userDetails) {

    User user = getPersistedUser(userDetails);

    return toProfileResponse(user);
  }

  @Transactional
  public UpdateUserProfileResponse updateUserProfile(
      CustomUserDetails userDetails,
      UpdateUserProfileRequest request) {

    User user = getPersistedUser(userDetails);

    String newEmail = request.getEmail()
        .trim()
        .toLowerCase(Locale.ROOT);

    boolean emailChanged = !newEmail.equalsIgnoreCase(user.getEmail());

    if (userRepository.existsByEmailIgnoreCaseAndIdNot(
        newEmail,
        user.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Email is already in use");
    }

    validateSocialLinks(request.getSocialLinks());

    user.setName(request.getName().trim());
    user.setSurname(request.getSurname().trim());
    user.setEmail(newEmail);
    user.setRecoveryEmail(normalizeNullableEmail(
        request.getRecoveryEmail()));

    ArtistProfile artistProfile = user.getArtistProfile();

    if (artistProfile == null && hasArtistData(request)) {

      artistProfile = ArtistProfile.builder()
          .user(user)
          .build();

      user.setArtistProfile(artistProfile);
    }

    if (artistProfile != null) {

      artistProfile.setUser(user);

      artistProfile.setType(request.getArtistType());

      artistProfile.setArtistName(
          normalizeNullableText(request.getArtistName()));

      artistProfile.setShortDescription(
          normalizeNullableText(request.getShortDescription()));

      artistProfile.setDescription(
          normalizeNullableText(request.getDescription()));

      artistProfile.setMainGenres(
          normalizeGenres(request.getMainGenres()));

      artistProfile.setAdditionalGenres(
          normalizeGenres(request.getAdditionalGenres()));

      artistProfile.setSocialLinks(
          toSocialLinksEntity(request.getSocialLinks()));
    }

    userRepository.save(user);

    UserProfileResponse profileResponse = toProfileResponse(user);

    return UpdateUserProfileResponse.builder()
        .profile(profileResponse)
        .emailChanged(emailChanged)
        .requiresReauthentication(emailChanged)
        .build();
  }

  public AvatarResponse uploadAvatar(
      CustomUserDetails userDetails,
      MultipartFile file) {

    validateAvatar(file);

    User user = getPersistedUser(userDetails);

    String oldAvatarFileName = user.getAvatarFileName();

    String newAvatarFileName = fileStorageService.saveAvatar(file);

    user.setAvatarFileName(newAvatarFileName);

    userRepository.save(user);

    if (oldAvatarFileName != null) {
      fileStorageService.deleteAvatar(oldAvatarFileName);
    }

    return AvatarResponse.builder()
        .avatarUrl(AVATAR_URL)
        .build();
  }

  public void deleteAvatar(
      CustomUserDetails userDetails) {

    User user = getPersistedUser(userDetails);

    String avatarFileName = user.getAvatarFileName();

    if (avatarFileName == null) {
      return;
    }

    fileStorageService.deleteAvatar(avatarFileName);

    user.setAvatarFileName(null);

    userRepository.save(user);
  }

  private User getPersistedUser(
      CustomUserDetails userDetails) {

    Long userId = userDetails.getUser().getId();

    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "User not found"));
  }

  private UserProfileResponse toProfileResponse(
      User user) {

    ArtistProfile artistProfile = user.getArtistProfile();

    return UserProfileResponse.builder()
        .name(user.getName())
        .surname(user.getSurname())
        .email(user.getEmail())
        .recoveryEmail(user.getRecoveryEmail())
        .dateOfBirth(user.getDateOfBirth())
        .artistType(
            artistProfile != null
                ? artistProfile.getType()
                : null)
        .artistName(
            artistProfile != null
                ? artistProfile.getArtistName()
                : null)
        .mainGenres(
            artistProfile != null
                && artistProfile.getMainGenres() != null
                    ? new ArrayList<>(artistProfile.getMainGenres())
                    : List.of())
        .additionalGenres(
            artistProfile != null
                && artistProfile.getAdditionalGenres() != null
                    ? new ArrayList<>(artistProfile.getAdditionalGenres())
                    : List.of())
        .shortDescription(
            artistProfile != null
                ? artistProfile.getShortDescription()
                : null)
        .description(
            artistProfile != null
                ? artistProfile.getDescription()
                : null)
        .socialLinks(
            artistProfile != null
                ? toSocialLinksDto(artistProfile.getSocialLinks())
                : null)
        .registeredAt(user.getCreatedAt())
        .avatarUrl(getAvatarUrl(user))
        .build();
  }

  private String getAvatarUrl(User user) {

    return fileStorageService.avatarExists(user.getAvatarFileName())
        ? AVATAR_URL
        : null;
  }

  private boolean hasArtistData(
      UpdateUserProfileRequest request) {

    return request.getArtistType() != null
        || hasText(request.getArtistName())
        || hasText(request.getShortDescription())
        || hasText(request.getDescription())
        || hasGenres(request.getMainGenres())
        || hasGenres(request.getAdditionalGenres())
        || hasSocialLinks(request.getSocialLinks());
  }

  private boolean hasGenres(List<String> genres) {

    return genres != null
        && genres.stream().anyMatch(this::hasText);
  }

  private boolean hasSocialLinks(
      ArtistSocialLinksDto socialLinks) {

    if (socialLinks == null) {
      return false;
    }

    return hasText(socialLinks.getTelegram())
        || hasText(socialLinks.getInstagram())
        || hasText(socialLinks.getYoutube())
        || hasText(socialLinks.getFacebook())
        || hasText(socialLinks.getSpotify())
        || hasText(socialLinks.getAppleMusic())
        || hasText(socialLinks.getSoundcloud())
        || hasText(socialLinks.getBandcamp())
        || hasText(socialLinks.getIcloud())
        || hasText(socialLinks.getTiktok());
  }

  private ArtistSocialLinks toSocialLinksEntity(
      ArtistSocialLinksDto dto) {

    if (dto == null) {
      return null;
    }

    String telegram = normalizeNullableText(dto.getTelegram());
    String instagram = normalizeNullableText(dto.getInstagram());
    String youtube = normalizeNullableText(dto.getYoutube());
    String facebook = normalizeNullableText(dto.getFacebook());
    String spotify = normalizeNullableText(dto.getSpotify());
    String appleMusic = normalizeNullableText(dto.getAppleMusic());
    String soundcloud = normalizeNullableText(dto.getSoundcloud());
    String bandcamp = normalizeNullableText(dto.getBandcamp());
    String icloud = normalizeNullableText(dto.getIcloud());
    String tiktok = normalizeNullableText(dto.getTiktok());

    if (telegram == null
        && instagram == null
        && youtube == null
        && facebook == null
        && spotify == null
        && appleMusic == null
        && soundcloud == null
        && bandcamp == null
        && icloud == null
        && tiktok == null) {
      return null;
    }

    return ArtistSocialLinks.builder()
        .telegram(telegram)
        .instagram(instagram)
        .youtube(youtube)
        .facebook(facebook)
        .spotify(spotify)
        .appleMusic(appleMusic)
        .soundcloud(soundcloud)
        .bandcamp(bandcamp)
        .icloud(icloud)
        .tiktok(tiktok)
        .build();
  }

  private ArtistSocialLinksDto toSocialLinksDto(
      ArtistSocialLinks links) {

    if (links == null) {
      return null;
    }

    return ArtistSocialLinksDto.builder()
        .telegram(links.getTelegram())
        .instagram(links.getInstagram())
        .youtube(links.getYoutube())
        .facebook(links.getFacebook())
        .spotify(links.getSpotify())
        .appleMusic(links.getAppleMusic())
        .soundcloud(links.getSoundcloud())
        .bandcamp(links.getBandcamp())
        .icloud(links.getIcloud())
        .tiktok(links.getTiktok())
        .build();
  }

  private void validateSocialLinks(
      ArtistSocialLinksDto links) {

    if (links == null) {
      return;
    }

    validateUrl(links.getTelegram(), "Telegram");
    validateUrl(links.getInstagram(), "Instagram");
    validateUrl(links.getYoutube(), "YouTube");
    validateUrl(links.getFacebook(), "Facebook");
    validateUrl(links.getSpotify(), "Spotify");
    validateUrl(links.getAppleMusic(), "Apple Music");
    validateUrl(links.getSoundcloud(), "SoundCloud");
    validateUrl(links.getBandcamp(), "Bandcamp");
    validateUrl(links.getIcloud(), "iCloud");
    validateUrl(links.getTiktok(), "TikTok");
  }

  private void validateUrl(
      String value,
      String fieldName) {

    if (value == null || value.isBlank()) {
      return;
    }

    try {

      URI uri = URI.create(value.trim());

      String scheme = uri.getScheme();

      if (scheme == null
          || (!scheme.equalsIgnoreCase("http")
              && !scheme.equalsIgnoreCase("https"))
          || uri.getHost() == null) {

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            fieldName + " must be a valid HTTP or HTTPS URL");
      }

    } catch (IllegalArgumentException e) {

      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          fieldName + " must be a valid HTTP or HTTPS URL");
    }
  }

  private List<String> normalizeGenres(
      List<String> genres) {

    if (genres == null) {
      return new ArrayList<>();
    }

    List<String> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    for (String genre : genres) {

      String normalized = normalizeNullableText(genre);

      if (normalized == null) {
        continue;
      }

      String key = normalized.toLowerCase(Locale.ROOT);

      if (seen.add(key)) {
        result.add(normalized);
      }
    }

    return result;
  }

  private String normalizeNullableEmail(String email) {

    String normalized = normalizeNullableText(email);

    return normalized != null
        ? normalized.toLowerCase(Locale.ROOT)
        : null;
  }

  private String normalizeNullableText(String value) {

    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }

  private boolean hasText(String value) {

    return value != null && !value.isBlank();
  }

  private void validateAvatar(MultipartFile file) {

    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Avatar file is empty");
    }

    long maxSize = 5L * 1024 * 1024;

    if (file.getSize() > maxSize) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Avatar file must not exceed 5 MB");
    }

    String contentType = file.getContentType();

    if (contentType == null
        || (!contentType.equals("image/jpeg")
            && !contentType.equals("image/png")
            && !contentType.equals("image/webp"))) {

      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Only JPEG, PNG and WebP images are allowed");
    }
  }
}