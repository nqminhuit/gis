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

    for (String s : items) {
      if (Pattern.compile(".*%s.*".formatted(regex)).matcher(s).matches()) {
        return s;
      }
    }
    return "";
  }
}
