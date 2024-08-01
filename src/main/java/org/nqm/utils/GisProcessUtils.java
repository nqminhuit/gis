package org.nqm.utils;

import static org.nqm.utils.StdOutUtils.warnln;
import java.io.File;
import java.io.IOException;
import org.nqm.config.GisLog;
import org.nqm.model.GisProcessDto;

public class GisProcessUtils {

  private GisProcessUtils() {}

  private static final String WARN_MSG_FMT = "Could not perform on module: '%s'";
  private static final String EXIT_WITH_CODE_MSG_FMT = "exit with code: '%s'";

  private static boolean dryRunEnabled;

  public static void isDryRunEnabled(boolean b) {
    dryRunEnabled = b;
  }

  private static void debugLogIfExitCodeNotZero(int exitCode, File directory) {
    if (exitCode == 0) {
      return;
    }
    GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
    warnln(WARN_MSG_FMT.formatted(directory.getName()));
  }

  public static GisProcessDto run(File directory, String... commands)
      throws IOException, InterruptedException {
    if (dryRunEnabled) {
      StdOutUtils.println(String.join(" ", commands));
      return GisProcessDto.EMPTY;
    }
    var p = new ProcessBuilder(commands).directory(directory).start();
    var exitCode = p.waitFor();
    debugLogIfExitCodeNotZero(exitCode, directory);
    return new GisProcessDto(new String(p.getInputStream().readAllBytes()), exitCode);
  }

  public static GisProcessDto quickRun(File directory, String... commands) throws IOException {
    if (dryRunEnabled) {
      StdOutUtils.println(String.join(" ", commands));
      return GisProcessDto.EMPTY;
    }
    var inputStream = new ProcessBuilder(commands).directory(directory).start().getInputStream();
    return new GisProcessDto(new String(inputStream.readAllBytes()), 0);
  }
}
