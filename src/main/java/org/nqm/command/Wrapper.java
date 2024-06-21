package org.nqm.command;

import static org.nqm.config.GisConfig.CURRENT_DIR;
import static org.nqm.utils.StdOutUtils.errln;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.nqm.config.GisLog;
import org.nqm.exception.GisException;
import org.nqm.vertx.CommandVerticle;

public final class Wrapper {

  private Wrapper() {}

  private static File isFileExist(File f) {
    return f.exists() ? f : null;
  }

  public static void deployVertx(Path path, String... args) {
    getFileMarker();
    new CommandVerticle(path, args).execute();
  }

  private static File getFileMarker() {
    File gitModulesFilePath;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var f1 = executor.submit(() -> isFileExist(Path.of(".", ".gis-modules").toFile()));
      var f2 = executor.submit(() -> isFileExist(Path.of(".", ".gitmodules").toFile()));
      gitModulesFilePath = Stream.of(f1.get(), f2.get())
          .filter(Objects::nonNull)
          .findFirst()
          .orElseThrow(
              () -> new GisException("Could not find '.gis-modules' or '.gitmodules' under this directory!"));
    } catch (InterruptedException | ExecutionException e) {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    }
    return gitModulesFilePath;
  }

  public static void forEachModuleWith(Predicate<Path> pred, String... args) {
    var gitModulesFilePath = getFileMarker();
    try (var exe = Executors.newVirtualThreadPerTaskExecutor()) {
      Files.readAllLines(gitModulesFilePath.toPath()).stream()
          .map(String::trim)
          .filter(s -> s.startsWith("path"))
          .map(s -> s.replace("path = ", ""))
          .map(dir -> Path.of(CURRENT_DIR, dir))
          .filter(dir -> {
            if (dir.toFile().exists()) {
              return true;
            }
            System.err.println("directory '%s' does not exist, will be ignored!".formatted("" + dir));
            return false;
          })
          .filter(pred)
          .forEach(dir -> exe.submit(() -> new CommandVerticle(dir, args).execute()));
    } catch (IOException e) {
      errln("failed to read file '.gis-modules' or '.gitmodules'");
    }
  }

  public static void forEachModuleDo(String... args) {
    forEachModuleWith(p -> true, args);
  }
}
