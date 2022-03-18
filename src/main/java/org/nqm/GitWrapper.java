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

  private static final String CURRENT_DIR = System.getProperty("user.dir");

  private GitWrapper() {}

  public static void status() {
    run(path -> call(path, true, "status", "-sb", "--ignore-submodules"), err::println);
  }

  public static void fetch() {
    run(path -> call(path, "fetch"), err::println);
  }

  public static void pull() {
    run(path -> call(path, "pull"), err::println);
  }

  public static void checkOut(String branch) {
    run(path -> call(path, "checkout", branch),
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

  private static Void call(Path path, boolean colorOutput, String... args) {
    if (!path.toFile().exists()) {
      // TODO if debug enable, print it out
      return null;
    }
    GisVertx.instance().deployVerticle(new CommandVerticle(path, colorOutput, args));
    return null;
  }

  private static Void call(Path path, String... args) {
    return call(path, false, args);
  }

  private static Stream<String> extractDirs(Buffer buffer) {
    return Stream.of(buffer.toString().split("\n"))
      .map(String::trim)
      .filter(s -> s.startsWith("path"))
      .map(s -> s.replace("path = ", ""));
  }

}
