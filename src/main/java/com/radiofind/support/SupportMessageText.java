package com.radiofind.support;

import org.springframework.web.util.HtmlUtils;

final class SupportMessageText {

  private SupportMessageText() {
  }

  static String of(String name, String email, String subject, String message) {
    return """
        <b>New Message</b>
        <b>Name: </b>%s
        <b>Email: </b>%s
        <b>Subject: </b>%s
        <b>Message: </b>%s
        """.formatted(escape(name), escape(email), escape(subject), escape(message));
  }

  private static String escape(String text) {
    return HtmlUtils.htmlEscape(text.trim());
  }
}
