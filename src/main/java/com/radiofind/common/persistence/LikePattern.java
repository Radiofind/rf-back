package com.radiofind.common.persistence;

import java.util.Locale;

public final class LikePattern {

  public static final char ESCAPE = '\\';

  private LikePattern() {
  }

  public static String containingIgnoreCase(String text) {
    String escaped = text.trim().toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");

    return "%" + escaped + "%";
  }
}
