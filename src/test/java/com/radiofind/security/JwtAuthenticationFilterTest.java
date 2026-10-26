package com.radiofind.security;

import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

  private static final String TOKEN = "jwt-token";

  private JwtService jwtService;

  private CustomUserDetailsService userDetailsService;

  private JwtAuthenticationFilter filter;

  private MockFilterChain filterChain;

  private final CustomUserDetails userDetails = new CustomUserDetails(
      User.builder().email("ada@example.com").role(Role.USER).build());

  @BeforeEach
  void setUp() {
    jwtService = mock(JwtService.class);
    userDetailsService = mock(CustomUserDetailsService.class);
    filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    filterChain = new MockFilterChain();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void doFilter(String authorizationHeader) throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");

    if (authorizationHeader != null) {
      request.addHeader("Authorization", authorizationHeader);
    }

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
  }

  private Authentication authentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  @Test
  void passesRequestWithoutAuthorizationHeader() throws Exception {
    doFilter(null);

    assertNull(authentication());
    assertNotNull(filterChain.getRequest());
    verifyNoInteractions(jwtService, userDetailsService);
  }

  @Test
  void ignoresNonBearerAuthorizationHeader() throws Exception {
    doFilter("Basic YWRhOnNlY3JldA==");

    assertNull(authentication());
    assertNotNull(filterChain.getRequest());
    verifyNoInteractions(jwtService, userDetailsService);
  }

  @Test
  void passesRequestWithInvalidTokenUnauthenticated() throws Exception {
    when(jwtService.extractUsername(TOKEN)).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

    doFilter("Bearer " + TOKEN);

    assertNull(authentication());
    assertNotNull(filterChain.getRequest());
    verifyNoInteractions(userDetailsService);
  }

  @Test
  void passesRequestUnauthenticatedWhenUserNoLongerExists() throws Exception {
    when(jwtService.extractUsername(TOKEN)).thenReturn("ada@example.com");
    when(userDetailsService.loadUserByUsername("ada@example.com"))
        .thenThrow(new UsernameNotFoundException("User not found"));

    doFilter("Bearer " + TOKEN);

    assertNull(authentication());
    assertNotNull(filterChain.getRequest());
    verify(jwtService, never()).isTokenValid(anyString(), any());
  }

  @Test
  void doesNotAuthenticateWhenTokenIsNotValidForUser() throws Exception {
    when(jwtService.extractUsername(TOKEN)).thenReturn("ada@example.com");
    when(userDetailsService.loadUserByUsername("ada@example.com")).thenReturn(userDetails);
    when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

    doFilter("Bearer " + TOKEN);

    assertNull(authentication());
    assertNotNull(filterChain.getRequest());
  }

  @Test
  void authenticatesRequestWithValidToken() throws Exception {
    when(jwtService.extractUsername(TOKEN)).thenReturn("ada@example.com");
    when(userDetailsService.loadUserByUsername("ada@example.com")).thenReturn(userDetails);
    when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

    doFilter("Bearer " + TOKEN);

    assertSame(userDetails, authentication().getPrincipal());
    assertNotNull(filterChain.getRequest());
  }

  @Test
  void keepsExistingAuthentication() throws Exception {
    TestingAuthenticationToken existing = new TestingAuthenticationToken("grace", null);
    SecurityContextHolder.getContext().setAuthentication(existing);
    when(jwtService.extractUsername(TOKEN)).thenReturn("ada@example.com");

    doFilter("Bearer " + TOKEN);

    assertSame(existing, authentication());
    verifyNoInteractions(userDetailsService);
  }
}
