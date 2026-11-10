package com.radiofind.track.schedule;

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
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaybackScheduleController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class PlaybackScheduleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PlaybackScheduleService playbackScheduleService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void changeToAFutureTimeIsAccepted() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    when(playbackScheduleService.requestChange(eq(ada), eq(42L), any()))
        .thenReturn(new PendingScheduleChange(Instant.parse("2099-01-01T18:00:00Z"), null, Instant.EPOCH));

    mockMvc.perform(post("/api/users/me/tracks/42/schedule-change-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"preferredAt\":\"2099-01-01T18:00:00Z\",\"comment\":\"Prime time\"}")
            .with(user(ada)))
        .andExpect(status().isCreated());

    verify(playbackScheduleService).requestChange(
        ada, 42L, new ScheduleChangeRequestBody(Instant.parse("2099-01-01T18:00:00Z"), "Prime time"));
  }

  @Test
  void changeToAPastTimeIsRejected() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(post("/api/users/me/tracks/42/schedule-change-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"preferredAt\":\"2000-01-01T18:00:00Z\"}")
            .with(user(ada)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(playbackScheduleService);
  }
}
