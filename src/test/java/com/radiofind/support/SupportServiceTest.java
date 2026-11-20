package com.radiofind.support;

import com.radiofind.support.telegram.TelegramClient;
import com.radiofind.support.telegram.TelegramDeliveryException;
import com.radiofind.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SupportServiceTest {

  @Test
  void messageOfAUserCarriesTheAccountNameAndEmail() {
    SupportMessageThrottle throttle = mock(SupportMessageThrottle.class);
    TelegramClient telegramClient = mock(TelegramClient.class);
    when(throttle.tryAcquire("user:7")).thenReturn(true);
    User ada = User.builder().id(7L).name("Ada").surname("Lovelace").email("ada@example.com").build();

    new SupportService(throttle, telegramClient).send(ada, new SupportMessageRequest("Upload stuck", "Help"));

    verify(telegramClient).sendMessage("""
        <b>New Message</b>
        <b>Name: </b>Ada Lovelace
        <b>Email: </b>ada@example.com
        <b>Subject: </b>Upload stuck
        <b>Message: </b>Help
        """);
  }

  @Test
  void nameOfAUserWithoutSurnameIsTheFirstName() {
    SupportMessageThrottle throttle = mock(SupportMessageThrottle.class);
    TelegramClient telegramClient = mock(TelegramClient.class);
    when(throttle.tryAcquire("user:7")).thenReturn(true);
    User ada = User.builder().id(7L).name("Ada").email("ada@example.com").build();

    new SupportService(throttle, telegramClient).send(ada, new SupportMessageRequest("Hi", "Hello"));

    verify(telegramClient).sendMessage("""
        <b>New Message</b>
        <b>Name: </b>Ada
        <b>Email: </b>ada@example.com
        <b>Subject: </b>Hi
        <b>Message: </b>Hello
        """);
  }

  @Test
  void contactMessageCarriesTheNameAndEmailOfTheForm() {
    SupportMessageThrottle throttle = mock(SupportMessageThrottle.class);
    TelegramClient telegramClient = mock(TelegramClient.class);
    when(throttle.tryAcquire("address:203.0.113.7")).thenReturn(true);

    new SupportService(throttle, telegramClient).sendContactMessage("203.0.113.7",
        new ContactMessageRequest("Grace", "grace@example.com", "Pricing", "Is there a yearly plan?"));

    verify(telegramClient).sendMessage("""
        <b>New Message</b>
        <b>Name: </b>Grace
        <b>Email: </b>grace@example.com
        <b>Subject: </b>Pricing
        <b>Message: </b>Is there a yearly plan?
        """);
  }

  @Test
  void contactMessageIsRefusedWhenTheAddressSentTooMany() {
    SupportMessageThrottle throttle = mock(SupportMessageThrottle.class);
    TelegramClient telegramClient = mock(TelegramClient.class);
    when(throttle.tryAcquire("address:203.0.113.7")).thenReturn(false);
    SupportService supportService = new SupportService(throttle, telegramClient);
    ContactMessageRequest request = new ContactMessageRequest("Grace", "grace@example.com", "Pricing", "Hi");

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> supportService.sendContactMessage("203.0.113.7", request));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
    verifyNoInteractions(telegramClient);
  }

  @Test
  void messageIsRefusedWhenTheUserSentTooMany() {
    SupportMessageThrottle throttle = mock(SupportMessageThrottle.class);
    TelegramClient telegramClient = mock(TelegramClient.class);
    when(throttle.tryAcquire("user:7")).thenReturn(false);
    User ada = User.builder().id(7L).email("ada@example.com").build();
    SupportService supportService = new SupportService(throttle, telegramClient);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> supportService.send(ada, new SupportMessageRequest("Upload stuck", "Help")));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
    verifyNoInteractions(telegramClient);
  }

  @Test
  void messageIsReportedUnavailableWhenTelegramFails() {
    SupportMessageThrottle throttle = mock(SupportMessageThrottle.class);
    TelegramClient telegramClient = mock(TelegramClient.class);
    when(throttle.tryAcquire("user:7")).thenReturn(true);
    doThrow(new TelegramDeliveryException("Telegram is unreachable")).when(telegramClient).sendMessage(anyString());
    User ada = User.builder().id(7L).name("Ada").email("ada@example.com").build();
    SupportService supportService = new SupportService(throttle, telegramClient);

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> supportService.send(ada, new SupportMessageRequest("Upload stuck", "Help")));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
  }
}
