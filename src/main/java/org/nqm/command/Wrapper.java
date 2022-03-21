package org.nqm.command;

import static org.nqm.config.GisConfig.CURRENT_DIR;
import static org.nqm.utils.StdOutUtils.errln;
import org.nqm.vertx.CommandVerticle;
import org.nqm.vertx.GisVertx;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;

public final class Wrapper {

  private Wrapper() {}

  public static void deployVertx(Path path, boolean colorOutput, String... args) {
    if (!path.toFile().exists()) {
      // TODO if debug enable, print it out
    }
    GisVertx.instance().deployVerticle(new CommandVerticle(path, colorOutput, args));
  }

  public static void deployVertx(Path path, String... args) {
    deployVertx(path, false, args);
  }

  public static void forEachModulesDo(Consumer<Path> consumeDir) {
    var gitModulesFilePath = Path.of(".", ".gitmodules");
    if (!gitModulesFilePath.toFile().exists()) {
      errln("There is no git submodules under this directory!");
      return;
    }

    GisVertx.instance().fileSystem().readFile(gitModulesFilePath.toString())
      .map(Wrapper::extractDirs)
      .onComplete((AsyncResult<Stream<String>> ar) -> {
        if (ar.succeeded()) {
          consumeDir.accept(Path.of(CURRENT_DIR));
          ar.result()
            .map(dir -> Path.of(CURRENT_DIR, dir))
            .filter(dir -> dir.toFile().exists())
            .forEach(dir -> consumeDir.accept(dir));
        }
        else {
          errln("failed to read file '.gitmodules'");
        }
      });
  }

  private static Stream<String> extractDirs(Buffer buffer) {
    return Stream.of(buffer.toString().split("\n"))
      .map(String::trim)
      .filter(s -> s.startsWith("path"))
      .map(s -> s.replace("path = ", ""));
  }

}
