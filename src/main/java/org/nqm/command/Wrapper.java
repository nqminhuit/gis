package org.nqm.command;

import org.nqm.vertx.CommandVerticle;
import org.nqm.vertx.GisVertx;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;

public final class Wrapper {

  private static final String CURRENT_DIR = System.getProperty("user.dir");

  private Wrapper() {}

  public static void execute(Function<Path, Void> consume) {
    var gitModulesFilePath = Path.of(".", ".gitmodules");
    if (!gitModulesFilePath.toFile().exists()) {
      throw new RuntimeException("There is no git submodules under this directory!");
    }

    var vertx = GisVertx.instance();
    vertx.fileSystem().readFile(gitModulesFilePath.toString())
      .map(Wrapper::extractDirs)
      .onComplete((AsyncResult<Stream<String>> ar) -> {
        if (ar.succeeded()) {
          ar.result().forEach(dir -> vertx.executeBlocking(
            (Promise<Void> p) -> p.complete(consume.apply(Path.of(CURRENT_DIR, dir)))));
          vertx.executeBlocking((Promise<Void> p) -> p.complete(consume.apply(Path.of(CURRENT_DIR))));
        }
        else {
          throw new RuntimeException("failed to read file");
        }
      });
  }

  public static Void deployVertx(Path path, boolean colorOutput, String... args) {
    if (!path.toFile().exists()) {
      // TODO if debug enable, print it out
      return null;
    }
    GisVertx.instance().deployVerticle(new CommandVerticle(path, colorOutput, args));
    return null;
  }

  public static Void deployVertx(Path path, String... args) {
    return deployVertx(path, false, args);
  }


  public static void getAllModuleDirs(Consumer<Path> consumeDir) {
    var gitModulesFilePath = Path.of(".", ".gitmodules");
    if (!gitModulesFilePath.toFile().exists()) {
      throw new RuntimeException("There is no git submodules under this directory!");
    }

    GisVertx.instance().fileSystem().readFile(gitModulesFilePath.toString())
      .map(Wrapper::extractDirs)
      .onComplete((AsyncResult<Stream<String>> ar) -> {
        if (ar.succeeded()) {
          ar.result()
            .map(dir -> Path.of(CURRENT_DIR, dir))
            .filter(dir -> dir.toFile().exists())
            .forEach(dir -> consumeDir.accept(dir));

          consumeDir.accept(Path.of(CURRENT_DIR));
        }
        else {
          throw new RuntimeException("failed to read file '.gitmodules'");
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
