package com.radiofind.user.controller;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.dto.*;
import com.radiofind.user.entity.User;
import com.radiofind.user.service.UserService;
import com.radiofind.storage.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final FileStorageService fileStorageService;

  @GetMapping("")
  public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userService.getCurrentUser(userDetails);
  }

  @GetMapping("/profile")
  public UserProfileResponse getUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userService.getUserProfile(userDetails);
  }

  @GetMapping("/avatar")
  public ResponseEntity<Resource> getAvatar(@AuthenticationPrincipal CustomUserDetails userDetails) {
    User user = userDetails.getUser();

    if (user.getAvatarFileName() == null) {
      return ResponseEntity.notFound().build();
    }

    Path avatarPath = fileStorageService.getAvatarPath(user.getAvatarFileName());

    if (!Files.exists(avatarPath)) {
      return ResponseEntity.notFound().build();
    }

    try {
      Resource resource = new UrlResource(avatarPath.toUri());

      String contentType = Files.probeContentType(avatarPath);

      MediaType mediaType = contentType != null
          ? MediaType.parseMediaType(contentType)
          : MediaType.APPLICATION_OCTET_STREAM;

      return ResponseEntity.ok()
          .contentType(mediaType)
          .body(resource);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read avatar file", e);
    }
  }

  @PostMapping(value = "/avatar", consumes = "multipart/form-data")
  public AvatarResponse uploadAvatar(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart("file") MultipartFile file) {
    return userService.uploadAvatar(userDetails, file);
  }

  @DeleteMapping("/avatar")
  public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal CustomUserDetails userDetails) {
    userService.deleteAvatar(userDetails);
    return ResponseEntity.noContent().build();
  }
}
