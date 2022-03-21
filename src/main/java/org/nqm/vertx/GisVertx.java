package org.nqm.vertx;

import static org.nqm.config.GisConfig.VERTX_OPTIONS;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class GisVertx {

  private static Vertx vertxInstance;
  private static EventBus eventBus;
  private static List<String> processedModules;

  public static final String ADDR_ADD_DIR = "add.dir";
  public static final String ADDR_REM_DIR = "remove.dir";

  public static Vertx instance() {
    if (vertxInstance == null) {
      vertxInstance = Vertx.vertx(VERTX_OPTIONS);
    }
    return vertxInstance;
  }

  private static List<String> processedModules() {
    if (processedModules == null) {
      processedModules = new ArrayList<>();
    }
    return processedModules;
  }

  public static EventBus eventBus() {
    if (eventBus == null) {
      eventBus = instance().eventBus();
    }

    eventBus.consumer(ADDR_ADD_DIR).handler(GisVertx::handleAddProcessedDir);
    eventBus.consumer(ADDR_REM_DIR).handler(GisVertx::handleRemoveProcessedDir);

    return eventBus;
  }

  private static void handleAddProcessedDir(Message<Object> msg) {
    processedModules().add("" + msg.body());
  }

  private static void handleRemoveProcessedDir(Message<Object> msg) {
    processedModules().removeIf(d -> d.equals("" + msg.body()));
    if (processedModules().size() < 1) {
      System.exit(0);
    }
  }

  public static void eventAddDir(Path dir) {
    eventBus().send(GisVertx.ADDR_ADD_DIR, "" + dir);
  }

  public static void eventRemoveDir(Path dir) {
    eventBus().send(GisVertx.ADDR_REM_DIR, "" + dir);
  }

}
