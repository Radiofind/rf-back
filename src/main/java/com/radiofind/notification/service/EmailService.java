package com.radiofind.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {

  private static final int TWO_FACTOR_CODE_EXPIRATION_MINUTES = 5;
  private static final int PASSWORD_RESET_EXPIRATION_MINUTES = 15;

  private final JavaMailSender mailSender;
  private final SpringTemplateEngine templateEngine;

  public void sendTwoFactorCode(String email, String code) {
    Context context = new Context();

    context.setVariable("code", code);

    context.setVariable("expirationMinutes", TWO_FACTOR_CODE_EXPIRATION_MINUTES);

    String htmlContent = templateEngine.process("email/two-factor-code", context);

    String plainText = """
        RADIOFIND

        Two-Factor Authentication

        Your verification code is:

        %s

        This code is valid for %d minutes.

        If you did not try to log in to RADIOFIND,
        you can safely ignore this email.
        """.formatted(code, TWO_FACTOR_CODE_EXPIRATION_MINUTES);

    sendEmail(email, "RADIOFIND - Two Factor Authentication Code", plainText, htmlContent);
  }

  public void sendPasswordResetLink(String email, String resetLink) {
    Context context = new Context();

    context.setVariable("resetLink", resetLink);

    context.setVariable("expirationMinutes", PASSWORD_RESET_EXPIRATION_MINUTES);

    String htmlContent = templateEngine.process("email/password-reset", context);

    String plainText = """
        RADIOFIND

        Password Reset

        We received a request to reset your RADIOFIND password.

        Open the following link to create a new password:

        %s

        This link is valid for %d minutes.

        If you did not request a password reset,
        you can safely ignore this email.
        """.formatted(resetLink, PASSWORD_RESET_EXPIRATION_MINUTES);

    sendEmail(email, "RADIOFIND - Password Reset", plainText, htmlContent);
  }

  private void sendEmail(String email, String subject, String plainText, String htmlContent) {
    try {
      var mimeMessage = mailSender.createMimeMessage();

      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

      helper.setTo(email);

      helper.setSubject(subject);

      helper.setText(plainText, htmlContent);

      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      throw new IllegalStateException("Failed to create email message", e);
    }
  }
}
