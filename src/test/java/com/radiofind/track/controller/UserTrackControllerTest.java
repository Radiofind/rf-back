package com.radiofind.track.controller;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.entity.TrackStatus;
import com.radiofind.track.service.TrackService;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserTrackController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class UserTrackControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackService trackService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void ownTracksAreFilteredByStatusAndSearchOfTheRequest() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    when(trackService.getOwnTracks(ada, TrackStatus.PENDING_REVIEW, "neon", 1, 7))
        .thenReturn(new PageResponse<>(List.of(TrackResponse.builder().id(42L).build()), 1, 7, 8, false));

    mockMvc.perform(get("/api/users/me/tracks")
            .param("status", "PENDING_REVIEW")
            .param("search", "neon")
            .param("page", "1")
            .param("size", "7")
            .with(user(ada)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(42))
        .andExpect(jsonPath("$.totalElements").value(8));
  }

  @Test
  void ownTracksRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/me/tracks"))
        .andExpect(status().isUnauthorized());
  }
}
