package org.nqm.utils;

import java.io.File;
import java.io.IOException;
import org.nqm.model.GisProcessDto;

public class GisProcessUtils {

  private GisProcessUtils() {}

  private static boolean dryRunEnabled;

  public static void isDryRunEnabled(boolean b) {
    dryRunEnabled = b;
  }

  public static GisProcessDto run(File directory, String... commands) throws IOException, InterruptedException {
    if (dryRunEnabled) {
      StdOutUtils.println(String.join(" ", commands));
      return new GisProcessDto("", 0);
    }
    var p = new ProcessBuilder(commands).directory(directory).start();
    return new GisProcessDto(new String(p.getInputStream().readAllBytes()), p.waitFor());
  }

  public static GisProcessDto quickRun(File directory, String... commands) throws IOException {
    if (dryRunEnabled) {
      StdOutUtils.println(String.join(" ", commands));
      return new GisProcessDto("", 0);
    }
    var inputStream = new ProcessBuilder(commands).directory(directory).start().getInputStream();
    return new GisProcessDto(new String(inputStream.readAllBytes()), 0);
  }
}
