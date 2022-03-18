package org.nqm;

import static java.lang.System.err;
import static org.nqm.utils.StdOutUtils.errln;
import static org.nqm.utils.StdOutUtils.warnln;
import org.nqm.vertx.CommandVerticle;
import org.nqm.vertx.GisVertx;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;

final class GitWrapper {

  private static final String GIT = "/usr/bin/git %s";

  private static final String CURRENT_DIR = System.getProperty("user.dir");

  private GitWrapper() {}

  public static void status() {
    run(path -> call(path, "status -sb --ignore-submodules", true), err::println);
  }

  public static void fetch() {
    run(path -> call(path, "fetch"), err::println);
  }

  public static void pull() {
    run(path -> call(path, "pull"), err::println);
  }

  public static void checkOut(String branch) {
    run(path -> call(path, "checkout %s".formatted(branch)),
      () -> errln("Could not checkout branch '%s'".formatted(branch)));
  }

  public static void checkOutNewBranch(String branch) {
    run(path -> call(path, "checkout -b %s".formatted(branch)), err::println);
  }


  private static void run(Function<Path, Void> consume, Runnable errHandling) {
    var gitModulesFilePath = Path.of(".", ".gitmodules");
    if (!gitModulesFilePath.toFile().exists()) {
      warnln("There is no git submodules under this directory!");
      return;
    }

    var vertx = GisVertx.instance();
    vertx.fileSystem().readFile(gitModulesFilePath.toString())
      .map(GitWrapper::extractDirs)
      .onComplete((AsyncResult<Stream<String>> ar) -> {
        if (ar.succeeded()) {
          ar.result().forEach(dir -> vertx.executeBlocking(
            (Promise<Void> p) -> p.complete(consume.apply(Path.of(CURRENT_DIR, dir)))));
          vertx.executeBlocking((Promise<Void> p) -> p.complete(consume.apply(Path.of(CURRENT_DIR))));
        }
        else {
          errln("failed to read file");
          System.exit(1);
        }
      });
  }

  private static Void call(Path path, String command, boolean colorOutput) {
    if (!path.toFile().exists()) {
      return null;
    }
    GisVertx.instance().deployVerticle(new CommandVerticle(GIT, command, path, colorOutput));
    return null;
  }

  private static Void call(Path path, String command) {
    return call(path, command, false);
  }

  private static Stream<String> extractDirs(Buffer buffer) {
    return Stream.of(buffer.toString().split("\n"))
      .map(String::trim)
      .filter(s -> s.startsWith("path"))
      .map(s -> s.replace("path = ", ""));
  }

}
