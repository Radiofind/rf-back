package com.radiofind.track.report;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackReportController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class TrackReportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackReportService trackReportService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void reportIsFiledForTheSignedInUser() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(post("/api/tracks/42/reports")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"SPAM\",\"comment\":\"Same loop\"}")
            .with(user(ada)))
        .andExpect(status().isNoContent());

    verify(trackReportService).report(ada, 42L, new TrackReportRequest(TrackReportReason.SPAM, "Same loop"));
  }

  @Test
  void reportWithoutReasonIsRejected() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(post("/api/tracks/42/reports")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"comment\":\"Same loop\"}")
            .with(user(ada)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(trackReportService);
  }

  @Test
  void reportingRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/tracks/42/reports")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"SPAM\"}"))
        .andExpect(status().isUnauthorized());
  }
}
