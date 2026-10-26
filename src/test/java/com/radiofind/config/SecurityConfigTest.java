package com.radiofind.config;

import com.radiofind.auth.controller.AuthController;
import com.radiofind.auth.dto.AuthResponse;
import com.radiofind.auth.service.AuthService;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.settings.controller.SettingsController;
import com.radiofind.settings.service.SettingsService;
import com.radiofind.storage.service.FileStorageService;
import com.radiofind.user.controller.UserController;
import com.radiofind.user.dto.CurrentUserResponse;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import com.radiofind.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = {AuthController.class, UserController.class, SettingsController.class},
    properties = "app.frontend-url=https://app.radiofind.test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {

  private static final String TOKEN = "jwt-token";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private FileStorageService fileStorageService;

  @MockitoBean
  private SettingsService settingsService;

  @Test
  void userEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/users/me/profile")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/users/me/avatar")).andExpect(status().isUnauthorized());
  }

  @Test
  void settingsEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/me/settings/general")).andExpect(status().isUnauthorized());
  }

  @Test
  void invalidTokenIsUnauthorized() throws Exception {
    when(jwtService.extractUsername(TOKEN)).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

    mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void validTokenGivesAccessToUserEndpoints() throws Exception {
    CustomUserDetails userDetails = new CustomUserDetails(
        User.builder().email("ada@example.com").role(Role.USER).build());
    when(jwtService.extractUsername(TOKEN)).thenReturn("ada@example.com");
    when(userDetailsService.loadUserByUsername("ada@example.com")).thenReturn(userDetails);
    when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);
    when(userService.getCurrentUser(userDetails))
        .thenReturn(CurrentUserResponse.builder().email("ada@example.com").build());

    mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("ada@example.com"));
  }

  @Test
  void authEndpointsArePublic() throws Exception {
    when(authService.login(any())).thenReturn(AuthResponse.builder().requiresTwoFactor(true).challengeId("c1").build());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ada@example.com\",\"password\":\"password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.challengeId").value("c1"));
  }

  @Test
  void invalidAuthRequestIsRejectedByValidation() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void corsAllowsConfiguredFrontendOrigin() throws Exception {
    mockMvc.perform(options("/api/auth/login")
            .header(HttpHeaders.ORIGIN, "https://app.radiofind.test")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.radiofind.test"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
  }

  @Test
  void corsRejectsUnknownOrigin() throws Exception {
    mockMvc.perform(options("/api/auth/login")
            .header(HttpHeaders.ORIGIN, "https://evil.test")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }
}
