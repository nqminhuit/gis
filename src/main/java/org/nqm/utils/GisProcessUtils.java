package org.nqm.utils;

import java.io.File;
import java.io.IOException;
import org.nqm.config.GisLog;
import org.nqm.exception.GisException;
import org.nqm.model.GisProcessDto;

public class GisProcessUtils {

  private GisProcessUtils() {}

  public static GisProcessDto run(File directory, String... commands) {
    Process p;
    try {
      p = new ProcessBuilder(commands).directory(directory).start();
    } catch (IOException e) {
      GisLog.debug(e);
      return null;
    }
    int exitCode = 1;
    try {
      exitCode = p.waitFor();
    } catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
    return new GisProcessDto(GisStringUtils.fromInputStream(p.getInputStream()), exitCode);
  }

  public static GisProcessDto quickRun(File directory, String... commands) {
    Process p;
    try {
      p = new ProcessBuilder(commands).directory(directory).start();
    } catch (IOException e) {
      GisLog.debug(e);
      return null;
    }
    return new GisProcessDto(GisStringUtils.fromInputStream(p.getInputStream()), 0);
  }

}
