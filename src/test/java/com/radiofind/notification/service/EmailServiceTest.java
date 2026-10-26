package com.radiofind.notification.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

  private JavaMailSender mailSender;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    mailSender = mock(JavaMailSender.class);
    when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));

    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);

    emailService = new EmailService(mailSender, templateEngine);
  }

  private MimeMessage sentMessage() {
    ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(message.capture());
    return message.getValue();
  }

  private static List<String> textParts(Part part) throws MessagingException, IOException {
    List<String> texts = new ArrayList<>();

    Object content = part.getContent();

    if (content instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); i++) {
        texts.addAll(textParts(multipart.getBodyPart(i)));
      }
    } else if (content instanceof String text) {
      texts.add(text);
    }

    return texts;
  }

  @Test
  void sendsTwoFactorCodeInPlainTextAndHtml() throws Exception {
    emailService.sendTwoFactorCode("ada@example.com", "042133");

    MimeMessage message = sentMessage();
    assertArrayEquals(new InternetAddress[] {new InternetAddress("ada@example.com")},
        message.getRecipients(Message.RecipientType.TO));
    assertEquals("RADIOFIND - Two Factor Authentication Code", message.getSubject());
    List<String> texts = textParts(message);
    assertEquals(2, texts.size());
    texts.forEach(text -> assertTrue(text.contains("042133"), text));
    assertTrue(texts.get(1).contains("<html"));
  }

  @Test
  void sendsPasswordResetLinkInPlainTextAndHtml() throws Exception {
    String resetLink = "https://radiofind.test/auth/reset-password?token=abc_123";

    emailService.sendPasswordResetLink("ada@example.com", resetLink);

    MimeMessage message = sentMessage();
    assertEquals("RADIOFIND - Password Reset", message.getSubject());
    List<String> texts = textParts(message);
    assertEquals(2, texts.size());
    texts.forEach(text -> assertTrue(text.contains(resetLink), text));
    assertTrue(texts.get(1).contains("href=\"" + resetLink + "\""));
  }
}
