package com.radiofind.support;

import com.radiofind.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

  private final SupportService supportService;

  @PostMapping("/messages")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void send(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody SupportMessageRequest request) {
    supportService.send(userDetails.getUser(), request);
  }

  @PostMapping("/contact-messages")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void sendContactMessage(HttpServletRequest httpRequest, @Valid @RequestBody ContactMessageRequest request) {
    supportService.sendContactMessage(httpRequest.getRemoteAddr(), request);
  }
}
