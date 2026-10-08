package com.radiofind.user.controller;

import com.radiofind.security.CustomUserDetails;
import com.radiofind.user.dto.CurrentUserResponse;
import com.radiofind.user.dto.UserProfileResponse;
import com.radiofind.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("")
  public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userService.getCurrentUser(userDetails);
  }

  @GetMapping("/profile")
  public UserProfileResponse getUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userService.getUserProfile(userDetails);
  }
}
