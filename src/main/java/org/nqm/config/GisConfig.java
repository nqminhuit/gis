package org.nqm.config;

import java.nio.file.Path;

public class GisConfig {

  private GisConfig() {}

  public static final String CURRENT_DIR = "" + Path.of("").toAbsolutePath();
  public static final String GIT_HOME_DIR = "/usr/bin/git";

}
