package com.radiofind.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupportMessageTextTest {

  @Test
  void userInputCannotInjectTelegramMarkup() {
    String text = SupportMessageText.of(
        "<i>Ada</i>", "ada@example.com", "<b>Urgent</b>", "Bass & drums < 3 dB");

    assertEquals("""
        <b>New Message</b>
        <b>Name: </b>&lt;i&gt;Ada&lt;/i&gt;
        <b>Email: </b>ada@example.com
        <b>Subject: </b>&lt;b&gt;Urgent&lt;/b&gt;
        <b>Message: </b>Bass &amp; drums &lt; 3 dB
        """, text);
  }

  @Test
  void surroundingWhitespaceIsTrimmed() {
    String text = SupportMessageText.of(" Ada Lovelace ", " ada@example.com ", "  Hi  ", "\n Hello \n");

    assertEquals("""
        <b>New Message</b>
        <b>Name: </b>Ada Lovelace
        <b>Email: </b>ada@example.com
        <b>Subject: </b>Hi
        <b>Message: </b>Hello
        """, text);
  }
}
