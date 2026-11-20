package com.radiofind.support.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class TelegramClient {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private static final String SEND_MESSAGE_PATH = "/bot{token}/sendMessage";

  private static final String PARSE_MODE = "HTML";

  private static final String EFFECT_DROPPED = "support message dropped";

  private final RestClient restClient;

  private final String botToken;

  private final String chatId;

  public TelegramClient(
      @Value("${app.telegram.api-url}") String apiUrl,
      @Value("${app.telegram.bot-token}") String botToken,
      @Value("${app.telegram.chat-id}") String chatId) {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    requestFactory.setReadTimeout(READ_TIMEOUT);

    this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory).build();
    this.botToken = botToken;
    this.chatId = chatId;
  }

  public void sendMessage(String htmlText) {
    requireSetting(botToken, "TELEGRAM_BOT_TOKEN");
    requireSetting(chatId, "TELEGRAM_CHAT_ID");

    Map<String, String> body = new LinkedHashMap<>();
    body.put("chat_id", chatId);
    body.put("parse_mode", PARSE_MODE);
    body.put("text", htmlText);

    try {
      restClient.post()
          .uri(SEND_MESSAGE_PATH, botToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.atError()
          .setMessage("telegram send failed")
          .addKeyValue("status", e.getStatusCode().value())
          .addKeyValue("cause", e.getResponseBodyAsString())
          .addKeyValue("effect", EFFECT_DROPPED)
          .log();
      throw new TelegramDeliveryException("Telegram rejected the message");
    } catch (ResourceAccessException e) {
      log.atError()
          .setMessage("telegram unreachable")
          .addKeyValue("cause", withoutRequestUrl(e))
          .addKeyValue("effect", EFFECT_DROPPED)
          .log();
      throw new TelegramDeliveryException("Telegram is unreachable");
    }
  }

  private static void requireSetting(String value, String variable) {
    if (value == null || value.isBlank()) {
      log.atError()
          .setMessage("telegram not configured")
          .addKeyValue("effect", EFFECT_DROPPED)
          .addKeyValue("fix", variable)
          .log();
      throw new TelegramDeliveryException("Telegram is not configured");
    }
  }

  private static String withoutRequestUrl(ResourceAccessException e) {
    Throwable cause = e.getMostSpecificCause();

    if (cause == e || cause.getMessage() == null) {
      return cause.getClass().getSimpleName();
    }

    return cause.getMessage();
  }
}
