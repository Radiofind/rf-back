package com.radiofind.track.controller;

import com.radiofind.config.SecurityConfig;
import com.radiofind.security.CustomUserDetails;
import com.radiofind.security.CustomUserDetailsService;
import com.radiofind.security.JwtAuthenticationFilter;
import com.radiofind.security.JwtService;
import com.radiofind.storage.service.TestImages;
import com.radiofind.track.dto.TrackResponse;
import com.radiofind.track.dto.UploadTrackRequest;
import com.radiofind.track.service.TrackAudio;
import com.radiofind.track.service.TrackCover;
import com.radiofind.track.service.TrackService;
import com.radiofind.user.entity.Role;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class TrackControllerTest {

  @TempDir
  Path coversPath;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TrackService trackService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void uploadTakesFilesAndDetailsFromOneMultipartRequest() throws Exception {
    CustomUserDetails ada = new CustomUserDetails(User.builder().id(7L).email("ada@example.com").role(Role.USER).build());
    MockMultipartFile audio = new MockMultipartFile("audio", "song.mp3", "audio/mpeg", new byte[] { 1 });
    when(trackService.upload(eq(ada), any(), any(), isNull()))
        .thenReturn(TrackResponse.builder().id(42L).build());
    ArgumentCaptor<UploadTrackRequest> request = ArgumentCaptor.forClass(UploadTrackRequest.class);

    mockMvc.perform(multipart("/api/tracks")
            .file(audio)
            .param("title", "Midnight Dreams")
            .param("genres", "GRUNGE", "INDIE_ROCK")
            .with(user(ada)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(42));

    verify(trackService).upload(eq(ada), request.capture(), any(), isNull());
    assertEquals("Midnight Dreams", request.getValue().getTitle());
    assertEquals(List.of("GRUNGE", "INDIE_ROCK"), request.getValue().getGenres());
  }

  @Test
  void coverIsPublicAndServedWithItsImageContentType() throws Exception {
    Path cover = Files.write(coversPath.resolve("c.png"), TestImages.PNG);
    when(trackService.getCover(42L)).thenReturn(new TrackCover(cover, MediaType.IMAGE_PNG));

    mockMvc.perform(get("/api/tracks/42/cover"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(TestImages.PNG));
  }

  @Test
  void audioIsStreamedWithoutLoginInTheRequestedByteRange() throws Exception {
    Path audio = Files.write(coversPath.resolve("a.mp3"), new byte[] { 1, 2, 3, 4, 5 });
    when(trackService.getAudio(42L, "signed"))
        .thenReturn(new TrackAudio(audio, MediaType.parseMediaType("audio/mpeg"), "Neon - Midnight.mp3"));

    mockMvc.perform(get("/api/tracks/42/audio").param("token", "signed").header("Range", "bytes=1-2"))
        .andExpect(status().isPartialContent())
        .andExpect(content().bytes(new byte[] { 2, 3 }));
  }

  @Test
  void downloadedAudioIsSavedUnderArtistAndTitle() throws Exception {
    Path audio = Files.write(coversPath.resolve("a.mp3"), new byte[] { 1 });
    when(trackService.getAudio(42L, "signed"))
        .thenReturn(new TrackAudio(audio, MediaType.parseMediaType("audio/mpeg"), "Neon Lights - Midnight Dreams.mp3"));

    mockMvc.perform(get("/api/tracks/42/audio").param("token", "signed").param("download", "true"))
        .andExpect(status().isOk())
        .andExpect(header().string(
            "Content-Disposition",
            "attachment; filename=\"=?UTF-8?Q?Neon_Lights_-_Midnight_Dreams.mp3?=\"; "
                + "filename*=UTF-8''Neon%20Lights%20-%20Midnight%20Dreams.mp3"));
  }

  @Test
  void uploadRequiresAuthentication() throws Exception {
    mockMvc.perform(multipart("/api/tracks")
            .file(new MockMultipartFile("audio", "song.mp3", "audio/mpeg", new byte[] { 1 }))
            .param("title", "Midnight Dreams")
            .param("genres", "GRUNGE"))
        .andExpect(status().isUnauthorized());
  }
}
