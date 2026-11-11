package com.radiofind.track.library;

import com.radiofind.common.dto.PageResponse;
import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class LibraryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private LibraryService libraryService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void libraryIsFilteredAndSortedAsRequested() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    when(libraryService.getLibrary(ada, new LibraryQuery("neon", LibrarySort.TITLE, Sort.Direction.ASC, 1, 20)))
        .thenReturn(new PageResponse<>(List.of(TrackResponse.builder().id(42L).build()), 1, 20, 21, false));

    mockMvc.perform(get("/api/users/me/library")
            .param("search", "neon")
            .param("sort", "TITLE")
            .param("direction", "ASC")
            .param("page", "1")
            .param("size", "20")
            .with(user(ada)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(42))
        .andExpect(jsonPath("$.totalElements").value(21));
  }

  @Test
  void libraryShowsTheLatestAdditionsFirstByDefault() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    when(libraryService.getLibrary(ada, new LibraryQuery(null, LibrarySort.ADDED, Sort.Direction.DESC, 0, 10)))
        .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, false));

    mockMvc.perform(get("/api/users/me/library").with(user(ada)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void libraryRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/me/library")).andExpect(status().isUnauthorized());
  }
}
