package com.radiofind.track.editing;

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
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackEditingController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class TrackEditingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackEditingService trackEditingService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void editTakesDetailsAndCoverFromOneMultipartRequest() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    ArgumentCaptor<EditTrackRequest> request = ArgumentCaptor.forClass(EditTrackRequest.class);

    mockMvc.perform(multipart("/api/users/me/tracks/42")
            .file(new MockMultipartFile("cover", "c.png", "image/png", new byte[] { 1 }))
            .param("title", "Midnight Dreams")
            .param("genres", "GRUNGE", "INDIE_ROCK")
            .param("removeCover", "true")
            .with(processor -> {
              processor.setMethod("PUT");
              return processor;
            })
            .with(user(ada)))
        .andExpect(status().isOk());

    verify(trackEditingService).edit(eq(ada), eq(42L), request.capture(), any());
    assertEquals("Midnight Dreams", request.getValue().getTitle());
    assertEquals(List.of("GRUNGE", "INDIE_ROCK"), request.getValue().getGenres());
    assertTrue(request.getValue().isRemoveCover());
  }
}
