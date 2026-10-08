package com.radiofind.user.service;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.dto.*;
import com.radiofind.user.entity.User;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.user.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final FileStorageService fileStorageService;

  UserService(UserRepository userRepository, FileStorageService fileStorageService) {
    this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
  }

  public CurrentUserResponse getCurrentUser(CustomUserDetails userDetails) {

    User user = userDetails.getUser();

    return CurrentUserResponse.builder()
        .name(user.getName())
        .surname(user.getSurname())
        .email(user.getEmail())
        .build();
  }

  public UserProfileResponse getUserProfile(CustomUserDetails userDetails) {

    User user = userDetails.getUser();

    return UserProfileResponse.builder()
        .name(user.getName())
        .surname(user.getSurname())
        .dateOfBirth(user.getDateOfBirth())
        .artistType(user.getArtistProfile() != null
            ? user.getArtistProfile().getType()
            : null)
        .artistName(user.getArtistProfile() != null
            ? user.getArtistProfile().getArtistName()
            : null)
        .description(user.getArtistProfile() != null
            ? user.getArtistProfile().getDescription()
            : null)
        .registeredAt(user.getCreatedAt())
        .avatarUrl(user.getAvatarFileName() != null
            ? "/api/users/me/avatar"
            : null)
        .build();
  }

  public AvatarResponse uploadAvatar(CustomUserDetails userDetails, MultipartFile file) {
    validateAvatar(file);

    User user = userDetails.getUser();

    String oldAvatarFileName = user.getAvatarFileName();

    String newAvatarFileName = fileStorageService.saveAvatar(file);

    user.setAvatarFileName(newAvatarFileName);

    userRepository.save(user);

    if (oldAvatarFileName != null) {
      fileStorageService.deleteAvatar(oldAvatarFileName);
    }

    return AvatarResponse.builder()
        .avatarUrl("/api/users/me/avatar")
        .build();
  }

  public void deleteAvatar(CustomUserDetails userDetails) {
    User user = userDetails.getUser();

    String avatarFileName = user.getAvatarFileName();

    if (avatarFileName == null) {
      return;
    }

    fileStorageService.deleteAvatar(avatarFileName);

    user.setAvatarFileName(null);

    userRepository.save(user);
  }

  private void validateAvatar(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("Avatar file is empty");
    }

    long maxSize = 5L * 1024 * 1024;

    if (file.getSize() > maxSize) {
      throw new IllegalArgumentException("Avatar file must not exceed 5 MB");
    }

    String contentType = file.getContentType();

    if (contentType == null) {
      throw new IllegalArgumentException("Avatar content type is missing");
    }

    if (!contentType.equals("image/jpeg")
        && !contentType.equals("image/png")
        && !contentType.equals("image/webh")) {
      throw new IllegalArgumentException("Only JPEG, PNG and WebP images are allowed");
    }
  }
}
