package com.radiofind.track.lifecycle;

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
import com.radiofind.track.entity.IllegalTrackStatusException;
import com.radiofind.track.entity.TrackStatus;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackLifecycleController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class TrackLifecycleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackLifecycleService trackLifecycleService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void submissionIsWithdrawnForTheSignedInOwner() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(post("/api/users/me/tracks/42/cancel-submission").with(user(ada)))
        .andExpect(status().isNoContent());

    verify(trackLifecycleService).cancelSubmission(ada, 42L);
  }

  @Test
  void transitionNotAllowedInTheCurrentStatusIsAConflict() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    doThrow(new IllegalTrackStatusException("submitted for review", TrackStatus.APPROVED))
        .when(trackLifecycleService).submitForReview(ada, 42L);

    mockMvc.perform(post("/api/users/me/tracks/42/submit").with(user(ada)))
        .andExpect(status().isConflict());
  }

  @Test
  void transitionsRequireAuthentication() throws Exception {
    mockMvc.perform(post("/api/users/me/tracks/42/submit")).andExpect(status().isUnauthorized());
  }
}
