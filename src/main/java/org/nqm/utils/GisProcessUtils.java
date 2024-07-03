package org.nqm.utils;

import java.io.File;
import java.io.IOException;
import org.nqm.model.GisProcessDto;

public class GisProcessUtils {

  private GisProcessUtils() {}

  public static GisProcessDto run(File directory, String... commands) throws IOException, InterruptedException {
    var p = new ProcessBuilder(commands).directory(directory).start();
    return new GisProcessDto(new String(p.getInputStream().readAllBytes()), p.waitFor());
  }

  public static GisProcessDto quickRun(File directory, String... commands) throws IOException {
    var inputStream = new ProcessBuilder(commands).directory(directory).start().getInputStream();
    return new GisProcessDto(new String(inputStream.readAllBytes()), 0);
  }
}
