package org.nqm.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class GisVertx {

  private static Vertx INSTANCE;

  public static Vertx instance() {
    if (INSTANCE == null) {
      var options = new VertxOptions();
      options.setEventLoopPoolSize(1);
      options.setWorkerPoolSize(1);
      options.setInternalBlockingPoolSize(1);
      INSTANCE = Vertx.vertx(options);
    }
    return INSTANCE;
  }

  // TODO maybe use EventBus (https://vertx.io/docs/vertx-core/java/#event_bus) to signal when to exit app
}
