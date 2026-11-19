package com.radiofind.track.download;

import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackDownloadController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class TrackDownloadControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackDownloadService trackDownloadService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void downloadIsRecordedForTheSignedInUser() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(post("/api/tracks/42/downloads").with(user(ada)))
        .andExpect(status().isNoContent());

    verify(trackDownloadService).recordDownload(ada, 42L);
  }

  @Test
  void downloadRecordingRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/tracks/42/downloads")).andExpect(status().isUnauthorized());

    verifyNoInteractions(trackDownloadService);
  }
}
