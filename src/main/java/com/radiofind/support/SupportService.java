package com.radiofind.support;

import com.radiofind.support.telegram.TelegramClient;
import com.radiofind.support.telegram.TelegramDeliveryException;
import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SupportService {

  private static final String USER_SENDER = "user:";

  private static final String ADDRESS_SENDER = "address:";

  private final SupportMessageThrottle throttle;

  private final TelegramClient telegramClient;

  public void send(User sender, SupportMessageRequest request) {
    deliver(USER_SENDER + sender.getId(), SupportMessageText.of(
        fullName(sender), sender.getEmail(), request.subject(), request.message()));
  }

  public void sendContactMessage(String clientAddress, ContactMessageRequest request) {
    deliver(ADDRESS_SENDER + clientAddress, SupportMessageText.of(
        request.name(), request.email(), request.subject(), request.message()));
  }

  private void deliver(String senderKey, String text) {
    if (!throttle.tryAcquire(senderKey)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many support messages, try again later");
    }

    try {
      telegramClient.sendMessage(text);
    } catch (TelegramDeliveryException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Support messages are unavailable", e);
    }
  }

  private static String fullName(User sender) {
    return Stream.of(sender.getName(), sender.getSurname())
        .filter(Objects::nonNull)
        .filter(part -> !part.isBlank())
        .collect(Collectors.joining(" "));
  }
}
