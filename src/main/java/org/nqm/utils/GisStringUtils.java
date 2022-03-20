package org.nqm.utils;

import java.util.List;
import java.util.regex.Pattern;

public class GisStringUtils {

  private GisStringUtils() {}

  public static boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }

  public static String convertToPathFromRegex(String regex, List<String> items) {
    if (".".equals(regex)) {
      return items.get(items.size() - 1);
    }

    var p = Pattern.compile(".*%s.*".formatted(regex));
    return items.stream()
      .filter(s -> p.matcher(s).matches())
      .findFirst()
      .orElse("");
  }
}
