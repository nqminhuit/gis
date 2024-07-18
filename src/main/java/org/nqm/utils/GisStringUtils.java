package org.nqm.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class GisStringUtils {

  private GisStringUtils() {}

  public static final String NEWLINE = "%n".formatted();

  public static boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }

  public static boolean isBlank(String s) {
    return !isNotBlank(s);
  }

  public static String getDirName(String dir) {
    return Path.of(dir).getFileName().toString();
  }

  public static InputStreamReader toInputStreamReader(String s) {
    return new InputStreamReader(new ByteArrayInputStream(s.getBytes()));
  }
}
