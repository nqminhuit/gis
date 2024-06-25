package org.nqm.command;

import static org.nqm.config.GisConfig.currentDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.nqm.config.GisLog;
import org.nqm.exception.GisException;
import org.nqm.utils.StdOutUtils;

public final class Wrapper {

  private Wrapper() {}

  private static File isFileExist(File f) {
    return f.exists() ? f : null;
  }

  private static File getFileMarker() {
    File gitModulesFilePath;
    var currentDir = currentDir();
    try (var exe = Executors.newVirtualThreadPerTaskExecutor()) {
      var f1 = exe.submit(() -> isFileExist(Path.of(currentDir, ".gis-modules").toFile()));
      var f2 = exe.submit(() -> isFileExist(Path.of(currentDir, ".gitmodules").toFile()));
      gitModulesFilePath = Stream.of(f1.get(), f2.get())
          .filter(Objects::nonNull)
          .findFirst()
          .orElseThrow(
              () -> new GisException("Could not find '.gis-modules' or '.gitmodules' under this directory!"));
    } catch (InterruptedException | ExecutionException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
    return gitModulesFilePath;
  }

  public static void forEachModuleWith(Predicate<Path> pred, String... args) {
    var gitModulesFilePath = getFileMarker();
    var currentDir = currentDir();
    try (var exe = Executors.newVirtualThreadPerTaskExecutor()) {
      Optional.of(Path.of(currentDir))
          .filter(pred)
          .ifPresent(root -> exe.submit(() -> CommandVerticle.execute(root, args)));
      Files.readAllLines(gitModulesFilePath.toPath()).stream()
          .map(String::trim)
          .filter(s -> s.startsWith("path"))
          .map(s -> s.replace("path = ", ""))
          .map(dir -> Path.of(currentDir, dir))
          .filter(dir -> {
            if (dir.toFile().exists()) {
              return true;
            }
            StdOutUtils.errln("directory '%s' does not exist, will be ignored!".formatted("" + dir));
            return false;
          })
          .filter(pred)
          .forEach(dir -> exe.submit(() -> CommandVerticle.execute(dir, args)));
    } catch (IOException e) {
      StdOutUtils.errln("failed to read file '.gis-modules' or '.gitmodules'");
    }
  }

  public static void forEachModuleDo(String... args) {
    forEachModuleWith(p -> true, args);
  }
}
