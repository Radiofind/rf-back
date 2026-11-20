package com.radiofind.support.telegram;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelegramClientTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  private String startTelegram(int status, AtomicReference<String> path, AtomicReference<String> body)
      throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      path.set(exchange.getRequestURI().getPath());
      body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] response = "{\"ok\":false,\"description\":\"Bad Request: chat not found\"}"
          .getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @Test
  void messageIsPostedToTheBotChatAsHtml() throws IOException {
    AtomicReference<String> path = new AtomicReference<>();
    AtomicReference<String> body = new AtomicReference<>();
    String apiUrl = startTelegram(200, path, body);

    new TelegramClient(apiUrl, "123:abc", "-100500").sendMessage("<b>New Message</b>");

    assertEquals("/bot123:abc/sendMessage", path.get());
    assertEquals("{\"chat_id\":\"-100500\",\"parse_mode\":\"HTML\",\"text\":\"<b>New Message</b>\"}", body.get());
  }

  @Test
  void rejectedMessageFailsDelivery() throws IOException {
    String apiUrl = startTelegram(400, new AtomicReference<>(), new AtomicReference<>());
    TelegramClient client = new TelegramClient(apiUrl, "123:abc", "-100500");

    assertThrows(TelegramDeliveryException.class, () -> client.sendMessage("<b>New Message</b>"));
  }

  @Test
  void unreachableTelegramFailsDelivery() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    TelegramClient client = new TelegramClient("http://127.0.0.1:" + closedPort, "123:abc", "-100500");

    assertThrows(TelegramDeliveryException.class, () -> client.sendMessage("<b>New Message</b>"));
  }

  @Test
  void missingBotTokenFailsDeliveryWithoutCallingTelegram() throws IOException {
    AtomicInteger calls = new AtomicInteger();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      calls.incrementAndGet();
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    TelegramClient client = new TelegramClient(
        "http://127.0.0.1:" + server.getAddress().getPort(), "", "-100500");

    assertThrows(TelegramDeliveryException.class, () -> client.sendMessage("<b>New Message</b>"));
    assertEquals(0, calls.get());
  }
}
