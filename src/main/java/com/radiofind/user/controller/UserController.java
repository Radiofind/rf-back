package com.radiofind.user.controller;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.dto.*;
import com.radiofind.user.service.UserService;
import com.radiofind.storage.service.AvatarFiles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final AvatarFiles avatarFiles;

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
    return avatarFiles.find(userDetails.getUser().getAvatarFileName())
        .map(avatar -> ResponseEntity.ok().contentType(avatar.mediaType()).body(avatar.resource()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping(value = "/avatar", consumes = "multipart/form-data")
  public AvatarResponse uploadAvatar(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestPart("file") MultipartFile file) {
    return userService.uploadAvatar(userDetails, file);
  }

  @PutMapping("/profile")
  public UpdateUserProfileResponse updateUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UpdateUserProfileRequest request) {
    return userService.updateUserProfile(userDetails, request);
  }

  @DeleteMapping("/avatar")
  public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal CustomUserDetails userDetails) {
    userService.deleteAvatar(userDetails);
    return ResponseEntity.noContent().build();
  }
}
