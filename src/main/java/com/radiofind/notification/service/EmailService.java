package com.radiofind.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  public void sendTwoFactorCode(String email, String code) {

    SimpleMailMessage message = new SimpleMailMessage();

    message.setTo(email);
    message.setSubject("RADIOFIND - Two Factor Authentication Code");

    message.setText(
        """
            Hello!

            Your RADIOFIND verifcation code is:

            %s

            This code is valid for 5 minutes.

            If you did not try to log in to RADIOFIND, please ignore this email.

            Best regards,
            RADIOFIND team
            """.formatted(code));

    mailSender.send(message);
  }
}
