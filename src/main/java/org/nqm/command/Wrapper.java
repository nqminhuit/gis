package org.nqm.command;

import static org.nqm.config.GisConfig.CURRENT_DIR;
import static org.nqm.utils.StdOutUtils.errln;
import static org.nqm.utils.StdOutUtils.warnln;
import java.io.File;
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

  public static void deployVertx(Path path, String... args) {
    getFileMarker();
    GisVertx.instance().deployVerticle(new CommandVerticle(path, args));
  }

  private static void deployEmptyVertx() {
    getFileMarker();
    GisVertx.instance().deployVerticle(new CommandVerticle());
  }

  private static File getFileMarker() {
    var gitModulesFilePath = Stream.of(Path.of(".", ".gis-modules"), Path.of(".", ".gitmodules"))
      .map(Path::toFile)
      .filter(File::exists)
      .findFirst()
      .orElse(null);

    if (gitModulesFilePath == null) {
      errln("Could not find '.gis-modules' or '.gitmodules' under this directory!");
      System.exit(1);
    }
    return gitModulesFilePath;
  }

  public static void forEachModuleWith(Predicate<Path> pred, Consumer<Path> consumeDir) {
    var gitModulesFilePath = getFileMarker();
    GisVertx.instance().fileSystem().readFile(gitModulesFilePath.toString())
      .map(Wrapper::extractDirs)
      .onComplete((AsyncResult<Stream<String>> ar) -> {
        if (ar.succeeded()) {
          shouldConsumeDirOrNot(pred, consumeDir, Path.of(CURRENT_DIR));
          ar.result()
            .map(dir -> Path.of(CURRENT_DIR, dir))
            .filter(dir -> {
              if (dir.toFile().exists()) {
                return true;
              }
              GisLog.debug("directory '%s' does not exist, will be ignored!".formatted("" + dir));
              return false;
            })
            .forEach(dir -> shouldConsumeDirOrNot(pred, consumeDir, dir));
        }
        else {
          errln("failed to read file '.gis-modules' or '.gitmodules'");
        }
      });
  }

  private static void shouldConsumeDirOrNot(Predicate<Path> pred, Consumer<Path> consumeDir, Path path) {
    if (pred.test(path)) {
      consumeDir.accept(path);
    }
    else {
      warnln("module '%s' does not satisfy the predicate".formatted("" + path.getFileName()));
      deployEmptyVertx();
    }
  }

  public static void forEachModuleDo(Consumer<Path> consumeDir) {
    forEachModuleWith(p -> true, consumeDir);
  }

  private static Stream<String> extractDirs(Buffer buffer) {
    return Stream.of(buffer.toString().split("\n"))
      .map(String::trim)
      .filter(s -> s.startsWith("path"))
      .map(s -> s.replace("path = ", ""));
  }

}
