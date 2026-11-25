package com.radiofind.statistics;

import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.statistics.StatisticsResponse.Totals;
import com.radiofind.statistics.period.Granularity;
import com.radiofind.statistics.period.StatisticsPeriod;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class StatisticsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private StatisticsService statisticsService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void statisticsAreShownForTheRequestedPeriodInTheViewersTimeZone() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    when(statisticsService.getStatistics(ada, StatisticsPeriod.LAST_12_MONTHS, ZoneId.of("Europe/Zurich")))
        .thenReturn(new StatisticsResponse(
            StatisticsPeriod.LAST_12_MONTHS,
            Granularity.MONTH,
            new Totals(120, 8, 14, 60),
            new Totals(100, 10, 7, 61),
            List.of(),
            List.of(),
            List.of()));

    mockMvc.perform(get("/api/users/me/statistics")
            .param("period", "LAST_12_MONTHS")
            .param("zone", "Europe/Zurich")
            .with(user(ada)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.current.plays").value(120))
        .andExpect(jsonPath("$.previous.uniqueListeners").value(61));
  }

  @Test
  void lastThirtyDaysInUtcAreShownByDefault() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    when(statisticsService.getStatistics(ada, StatisticsPeriod.LAST_30_DAYS, ZoneId.of("UTC")))
        .thenReturn(new StatisticsResponse(
            StatisticsPeriod.LAST_30_DAYS, Granularity.DAY, null, null, List.of(), List.of(), List.of()));

    mockMvc.perform(get("/api/users/me/statistics").with(user(ada)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.period").value("LAST_30_DAYS"));
  }

  @Test
  void unknownTimeZoneIsRejected() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());

    mockMvc.perform(get("/api/users/me/statistics").param("zone", "Mars/Olympus").with(user(ada)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(statisticsService);
  }

  @Test
  void statisticsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/me/statistics")).andExpect(status().isUnauthorized());

    verifyNoInteractions(statisticsService);
  }
}
