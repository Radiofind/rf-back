package com.radiofind.track.controller;

import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.track.service.TrackEngagementService;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackEngagementController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class TrackEngagementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackEngagementService trackEngagementService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void listenIsRecordedForTheSignedInUser() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(post("/api/tracks/42/plays").with(user(ada)))
        .andExpect(status().isNoContent());

    verify(trackEngagementService).recordPlay(ada, 42L);
  }

  @Test
  void likeIsSetWithPutAndRemovedWithDelete() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(put("/api/tracks/42/like").with(user(ada))).andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/tracks/42/like").with(user(ada))).andExpect(status().isNoContent());

    verify(trackEngagementService).like(ada, 42L);
    verify(trackEngagementService).unlike(ada, 42L);
  }

  @Test
  void engagementRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/tracks/42/plays")).andExpect(status().isUnauthorized());
    mockMvc.perform(put("/api/tracks/42/like")).andExpect(status().isUnauthorized());

    verifyNoInteractions(trackEngagementService);
  }
}
