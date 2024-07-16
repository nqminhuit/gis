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

  public static InputStreamReader toInputStreamReader(String s) {
    return new InputStreamReader(new ByteArrayInputStream(s.getBytes()));
  }

  public static String getDirectoryName(Path p) {
    return "" + p.subpath(p.getNameCount() - 1, p.getNameCount());
  }
}
