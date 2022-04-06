package org.nqm.command;

import static org.nqm.config.GisConfig.CURRENT_DIR;
import static org.nqm.utils.StdOutUtils.errln;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.nqm.config.GisLog;
import org.nqm.vertx.CommandVerticle;
import org.nqm.vertx.GisVertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;

public final class Wrapper {

  private Wrapper() {}

  public static void deployVertx(Path path, boolean colorOutput, String... args) {
    if (!path.toFile().exists()) {
      GisLog.debug("directory '%s' does not exist!".formatted("" + path));
    }
    GisVertx.instance().deployVerticle(new CommandVerticle(path, colorOutput, args));
  }

  public static void deployVertx(Path path, String... args) {
    deployVertx(path, false, args);
  }

  private static void forEachModuleWith(Predicate<Path> pred, Consumer<Path> consumeDir, boolean includeRoot) {
    var gitModulesFilePath = Path.of(".", ".gitmodules");
    if (!gitModulesFilePath.toFile().exists()) {
      errln("There is no git submodules under this directory!");
      return;
    }

    GisVertx.instance().fileSystem().readFile(gitModulesFilePath.toString())
      .map(Wrapper::extractDirs)
      .onComplete((AsyncResult<Stream<String>> ar) -> {
        if (ar.succeeded()) {
          shouldConsumeDirOrNot(pred.and(x -> includeRoot), consumeDir, Path.of(CURRENT_DIR));
          ar.result()
            .map(dir -> Path.of(CURRENT_DIR, dir))
            .filter(dir -> dir.toFile().exists())
            .forEach(dir -> shouldConsumeDirOrNot(pred, consumeDir, dir));
        }
        else {
          errln("failed to read file '.gitmodules'");
        }
      });
  }

  public static void forEachModuleWith(Predicate<Path> pred, Consumer<Path> consumeDir) {
    forEachModuleWith(pred, consumeDir, true);
  }

  private static void shouldConsumeDirOrNot(Predicate<Path> pred, Consumer<Path> consumeDir, Path path) {
    if (pred.test(path)) {
      consumeDir.accept(path);
    }
    else {
      GisLog.debug("directory '%s' does not satisfy the predicate".formatted("" + path));
    }
  }

  public static void forEachModuleDo(Consumer<Path> consumeDir) {
    forEachModuleWith(p -> true, consumeDir);
  }

  public static void forEachSubmoduleDo(Consumer<Path> consumeDir) {
    forEachModuleWith(p -> true, consumeDir, false);
  }

  private static Stream<String> extractDirs(Buffer buffer) {
    return Stream.of(buffer.toString().split("\n"))
      .map(String::trim)
      .filter(s -> s.startsWith("path"))
      .map(s -> s.replace("path = ", ""));
  }

}
