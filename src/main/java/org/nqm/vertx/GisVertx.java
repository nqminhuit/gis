package org.nqm.vertx;

import static org.nqm.config.GisConfig.VERTX_OPTIONS;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.nqm.config.GisLog;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class GisVertx {

  private static Vertx vertxInstance;
  private static EventBus eventBus;
  private static Map<String, Boolean> processedModules;

  public static final String ADDR_ADD_DIR = "add.dir";
  public static final String ADDR_REM_DIR = "remove.dir";

  private GisVertx() {}

  public static Vertx instance() {
    if (vertxInstance == null) {
      vertxInstance = Vertx.vertx(VERTX_OPTIONS);
    }
    return vertxInstance;
  }

  private static Map<String, Boolean> processedModules() {
    if (processedModules == null) {
      processedModules = new HashMap<>();
    }
    return processedModules;
  }

  public static EventBus eventBus() {
    if (eventBus == null) {
      eventBus = instance().eventBus();
    }

    eventBus.consumer(ADDR_ADD_DIR).handler(GisVertx::addNewModule);
    eventBus.consumer(ADDR_REM_DIR).handler(GisVertx::processedModule);

    return eventBus;
  }

  private static void addNewModule(Message<Object> msg) {
    processedModules().put("" + msg.body(), false);
  }

  private static void processedModule(Message<Object> msg) {
    var modules = processedModules();
    modules.computeIfPresent("" + msg.body(), (k, v) -> true);
    if (!modules.isEmpty() && modules.values().stream().allMatch(Boolean.TRUE::equals)) {
      System.exit(0);
    }
  }

  public static void eventAddDir(Path dir) {
    GisLog.debug("+++ adding module '%s' to queue".formatted("" + dir.getFileName()));
    eventBus().send(GisVertx.ADDR_ADD_DIR, "" + dir);
  }

  public static void eventRemoveDir(Path dir) {
    GisLog.debug("--- removing module '%s' from queue".formatted("" + dir.getFileName()));
    eventBus().send(GisVertx.ADDR_REM_DIR, "" + dir);
  }

}
