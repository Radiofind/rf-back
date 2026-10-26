package com.radiofind.user.controller;

import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.storage.service.TestImages;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import com.radiofind.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class UserControllerTest {

  @TempDir
  Path avatarsPath;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private FileStorageService fileStorageService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    when(fileStorageService.getAvatarPath(anyString()))
        .thenAnswer(invocation -> avatarsPath.resolve(invocation.<String>getArgument(0)));
  }

  private CustomUserDetails userWithAvatar(String avatarFileName) {
    return new CustomUserDetails(User.builder()
        .email("ada@example.com")
        .role(Role.USER)
        .avatarFileName(avatarFileName)
        .build());
  }

  @Test
  void avatarIsNotFoundWhenUserHasNoAvatar() throws Exception {
    mockMvc.perform(get("/api/users/me/avatar").with(user(userWithAvatar(null))))
        .andExpect(status().isNotFound());
  }

  @Test
  void avatarIsNotFoundWhenFileIsMissing() throws Exception {
    mockMvc.perform(get("/api/users/me/avatar").with(user(userWithAvatar("lost.png"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void avatarIsServedWithImageContentType() throws Exception {
    Files.write(avatarsPath.resolve("avatar.png"), TestImages.PNG);

    mockMvc.perform(get("/api/users/me/avatar").with(user(userWithAvatar("avatar.png"))))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(TestImages.PNG))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  void legacyAvatarWithUnsafeExtensionIsNotServedAsHtml() throws Exception {
    Files.write(avatarsPath.resolve("legacy.html"), TestImages.HTML);

    mockMvc.perform(get("/api/users/me/avatar").with(user(userWithAvatar("legacy.html"))))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
  }
}
