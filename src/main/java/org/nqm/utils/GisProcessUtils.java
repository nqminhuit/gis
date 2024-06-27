package org.nqm.utils;

import java.io.File;
import java.io.IOException;
import org.nqm.config.GisLog;
import org.nqm.exception.GisException;
import org.nqm.model.GisProcessDto;

public class GisProcessUtils {

  private GisProcessUtils() {}

  public static GisProcessDto run(File directory, String... commands) throws IOException {
    var p = new ProcessBuilder(commands).directory(directory).start();
    int exitCode = 1;
    try {
      exitCode = p.waitFor();
    } catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
    return new GisProcessDto(new String(p.getInputStream().readAllBytes()), exitCode);
  }

  public static GisProcessDto quickRun(File directory, String... commands) throws IOException {
    var inputStream = new ProcessBuilder(commands).directory(directory).start().getInputStream();
    return new GisProcessDto(new String(inputStream.readAllBytes()), 0);
  }
}
