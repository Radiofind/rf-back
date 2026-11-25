package com.radiofind.profile.controller;

import com.radiofind.config.SecurityConfig;
import com.radiofind.profile.service.ProfileFollowService;
import com.radiofind.profile.service.PublicProfileService;
import com.radiofind.profile.service.PublicTrackService;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.storage.service.AvatarImage;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicProfileController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class PublicProfileControllerTest {

  private static final UUID PUBLIC_ID = UUID.fromString("6f1c2b4e-3d5a-4c8e-9b7f-1a2b3c4d5e6f");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PublicProfileService publicProfileService;

  @MockitoBean
  private PublicTrackService publicTrackService;

  @MockitoBean
  private ProfileFollowService profileFollowService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void profileEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/{id}/profile", PUBLIC_ID)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/users/{id}/tracks", PUBLIC_ID)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/users/{id}/avatar", PUBLIC_ID)).andExpect(status().isUnauthorized());
    mockMvc.perform(put("/api/users/{id}/follow", PUBLIC_ID)).andExpect(status().isUnauthorized());

    verifyNoInteractions(publicProfileService, publicTrackService, profileFollowService);
  }

  @Test
  void profileIsAddressedOnlyByPublicId() throws Exception {
    mockMvc.perform(get("/api/users/42/profile").with(user(ada()))).andExpect(status().isBadRequest());

    verifyNoInteractions(publicProfileService);
  }

  @Test
  void followIsSetWithPutAndRemovedWithDelete() throws Exception {
    CustomUserDetails ada = ada();

    mockMvc.perform(put("/api/users/{id}/follow", PUBLIC_ID).with(user(ada))).andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/users/{id}/follow", PUBLIC_ID).with(user(ada))).andExpect(status().isNoContent());

    verify(profileFollowService).follow(ada, PUBLIC_ID);
    verify(profileFollowService).unfollow(ada, PUBLIC_ID);
  }

  @Test
  void avatarIsServedWithItsImageType() throws Exception {
    CustomUserDetails ada = ada();
    when(publicProfileService.getAvatar(ada, PUBLIC_ID))
        .thenReturn(Optional.of(new AvatarImage(new ByteArrayResource(new byte[] { 1, 2 }), MediaType.IMAGE_PNG)));

    mockMvc.perform(get("/api/users/{id}/avatar", PUBLIC_ID).with(user(ada)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(new byte[] { 1, 2 }));
  }

  @Test
  void avatarThatMayNotBeShownIsNotFound() throws Exception {
    CustomUserDetails ada = ada();
    when(publicProfileService.getAvatar(ada, PUBLIC_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/users/{id}/avatar", PUBLIC_ID).with(user(ada))).andExpect(status().isNotFound());
  }

  @Test
  void tracksPageSizeIsCapped() throws Exception {
    mockMvc.perform(get("/api/users/{id}/tracks", PUBLIC_ID).param("size", "51").with(user(ada())))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(publicTrackService);
  }

  private static CustomUserDetails ada() {
    return new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
  }
}
