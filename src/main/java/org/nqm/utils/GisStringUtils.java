package org.nqm.utils;

public class GisStringUtils {

  private GisStringUtils() {}

  public static boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }

  public static boolean isBlank(String s) {
    return !isNotBlank(s);
  }
}
