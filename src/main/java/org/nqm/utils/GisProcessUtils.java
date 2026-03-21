package org.nqm.utils;

import static org.nqm.utils.StdOutUtils.warnln;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
    var pb = new ProcessBuilder(commands).directory(directory);
    var p = pb.start();

    var outBaos = new ByteArrayOutputStream();
    var errBaos = new ByteArrayOutputStream();

    var tOut = Thread.ofVirtual().start(() -> {
      try (var is = p.getInputStream()) {
        is.transferTo(outBaos);
      } catch (IOException e) {
        GisLog.debug(e);
      }
    });

    var tErr = Thread.ofVirtual().start(() -> {
      try (var es = p.getErrorStream()) {
        es.transferTo(errBaos);
      } catch (IOException e) {
        GisLog.debug(e);
      }
    });

    var exitCode = p.waitFor();
    tOut.join();
    tErr.join();

    debugLogIfExitCodeNotZero(exitCode, directory);
    var stdout = outBaos.toString(StandardCharsets.UTF_8);
    var stderr = errBaos.toString(StandardCharsets.UTF_8);
    if (GisStringUtils.isNotBlank(stderr)) {
      GisLog.debug(stderr);
    }
    return new GisProcessDto(stdout, exitCode);
  }

  public static GisProcessDto quickRun(File directory, String... commands) throws IOException, InterruptedException {
    if (dryRunEnabled) {
      StdOutUtils.println(String.join(" ", commands));
      return GisProcessDto.EMPTY;
    }
    var pb = new ProcessBuilder(commands).directory(directory);
    var p = pb.start();

    var outBaos = new ByteArrayOutputStream();
    var errBaos = new ByteArrayOutputStream();

    var tOut = Thread.ofVirtual().start(() -> {
      try (var is = p.getInputStream()) {
        is.transferTo(outBaos);
      } catch (IOException e) {
        GisLog.debug(e);
      }
    });

    var tErr = Thread.ofVirtual().start(() -> {
      try (var es = p.getErrorStream()) {
        es.transferTo(errBaos);
      } catch (IOException e) {
        GisLog.debug(e);
      }
    });

    var exitCode = p.waitFor();
    tOut.join();
    tErr.join();

    var stdout = outBaos.toString(StandardCharsets.UTF_8);
    var stderr = errBaos.toString(StandardCharsets.UTF_8);
    if (GisStringUtils.isNotBlank(stderr)) {
      GisLog.debug(stderr);
    }
    return new GisProcessDto(stdout, exitCode);
  }

  public static void spawn(File directory, String... commands) throws IOException {
    if (dryRunEnabled) {
      StdOutUtils.println(String.join(" ", commands));
      return;
    }
    new ProcessBuilder(commands)
        .directory(directory)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
  }
}
