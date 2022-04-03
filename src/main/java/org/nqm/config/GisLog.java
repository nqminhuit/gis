package org.nqm.config;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;

public class GisLog {

  private static Boolean isDebugEnabled;

  public static void setIsDebugEnabled(boolean b) {
    if (isDebugEnabled == null) {
      isDebugEnabled = b;
    }
  }

  private static void debug(String msg, Throwable e) {
    if (Boolean.TRUE.equals(isDebugEnabled)) {
      Optional.ofNullable(msg).filter(GisStringUtils::isNotBlank).ifPresent(StdOutUtils::debugln);
      Optional.ofNullable(e).ifPresent(Throwable::printStackTrace);
    }
  }

  public static void debug(String msg) {
    GisLog.debug(msg, null);
  }

  public static void debug(Throwable e) {
    GisLog.debug(null, e);
  }

  public static void debug(String msgFormat, String[] args, Path path) {
    if (Boolean.TRUE.equals(isDebugEnabled)) {
      StdOutUtils.debugln(msgFormat.formatted(Stream.of(args).collect(Collectors.joining(" ")), path));
    }
  }
}
