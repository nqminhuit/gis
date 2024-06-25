package org.nqm.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.nqm.config.GisLog;

public class GisStringUtils {

  private GisStringUtils() {}

  public static boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }

  public static boolean isBlank(String s) {
    return !isNotBlank(s);
  }

  public static String fromInputStream(InputStream stream) {
    try {
      return new String(stream.readAllBytes());
    } catch (IOException e) {
      GisLog.debug(e);
      return "";
    }
  }

  public static InputStreamReader toInputStreamReader(String s) {
    return new InputStreamReader(new ByteArrayInputStream(s.getBytes()));
  }
}
