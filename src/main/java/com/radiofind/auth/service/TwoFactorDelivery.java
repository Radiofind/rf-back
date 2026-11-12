package com.radiofind.auth.service;

import com.radiofind.auth.entity.TwoFactorMethod;
import com.radiofind.notification.service.EmailService;
import com.radiofind.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TwoFactorDelivery {

  private static final int VISIBLE_LOCAL_PART = 2;
  private static final String MASK = "***";

  private final EmailService emailService;

  public void send(User user, TwoFactorMethod method, String code) {
    String address = addressOf(user, method);

    if (address != null) {
      emailService.sendTwoFactorCode(address, code);
    }
  }

  public String maskedDestination(User user, TwoFactorMethod method) {
    return mask(addressOf(user, method));
  }

  public String mask(String address) {
    if (address == null) {
      return null;
    }

    int at = address.indexOf('@');

    if (at <= 0) {
      return MASK;
    }

    return address.substring(0, Math.min(VISIBLE_LOCAL_PART, at)) + MASK + address.substring(at);
  }

  private String addressOf(User user, TwoFactorMethod method) {
    return switch (method) {
      case EMAIL -> user.getEmail();
      case RECOVERY_EMAIL -> user.getRecoveryEmail();
      case AUTHENTICATOR -> null;
    };
  }
}
