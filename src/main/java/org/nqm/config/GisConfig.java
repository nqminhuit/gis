package org.nqm.config;

import io.vertx.core.VertxOptions;

public class GisConfig {

  private GisConfig() {}

  public static final String CURRENT_DIR = System.getProperty("user.dir");
  public static final VertxOptions VERTX_OPTIONS = vertxOptions();
  public static final String GIT_HOME_DIR = "/usr/bin/git";

  private static VertxOptions vertxOptions() {
    var options = new VertxOptions();
    options.setEventLoopPoolSize(1);
    options.setWorkerPoolSize(1);
    options.setInternalBlockingPoolSize(1);
    options.setBlockedThreadCheckInterval(1000000);
    return options;
  }

}
