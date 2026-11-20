package com.radiofind.support;

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

@WebMvcTest(SupportController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class SupportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SupportService supportService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void messageIsSentOnBehalfOfTheSignedInUser() throws Exception {
    User ada = User.builder().id(7L).email("ada@example.com").role(Role.USER).build();

    mockMvc.perform(post("/api/support/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Upload stuck\",\"message\":\"My track stays in review\"}")
            .with(user(new CustomUserDetails(ada))))
        .andExpect(status().isNoContent());

    verify(supportService).send(ada, new SupportMessageRequest("Upload stuck", "My track stays in review"));
  }

  @Test
  void messageWithoutSubjectIsRejected() throws Exception {
    User ada = User.builder().id(7L).email("ada@example.com").role(Role.USER).build();

    mockMvc.perform(post("/api/support/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"  \",\"message\":\"My track stays in review\"}")
            .with(user(new CustomUserDetails(ada))))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(supportService);
  }

  @Test
  void messageLongerThanTheLimitIsRejected() throws Exception {
    User ada = User.builder().id(7L).email("ada@example.com").role(Role.USER).build();

    mockMvc.perform(post("/api/support/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Upload stuck\",\"message\":\"" + "a".repeat(2001) + "\"}")
            .with(user(new CustomUserDetails(ada))))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(supportService);
  }

  @Test
  void sendingRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/support/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Upload stuck\",\"message\":\"My track stays in review\"}"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(supportService);
  }

  @Test
  void contactMessageIsAcceptedWithoutSignInAndKeyedByTheCallerAddress() throws Exception {
    mockMvc.perform(post("/api/support/contact-messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Grace\",\"email\":\"grace@example.com\","
                + "\"subject\":\"Pricing\",\"message\":\"Is there a yearly plan?\"}")
            .with(request -> {
              request.setRemoteAddr("203.0.113.7");
              return request;
            }))
        .andExpect(status().isNoContent());

    verify(supportService).sendContactMessage("203.0.113.7",
        new ContactMessageRequest("Grace", "grace@example.com", "Pricing", "Is there a yearly plan?"));
  }

  @Test
  void contactMessageWithMalformedEmailIsRejected() throws Exception {
    mockMvc.perform(post("/api/support/contact-messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Grace\",\"email\":\"grace\","
                + "\"subject\":\"Pricing\",\"message\":\"Is there a yearly plan?\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(supportService);
  }
}
