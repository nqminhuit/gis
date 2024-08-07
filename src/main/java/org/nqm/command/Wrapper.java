package org.nqm.command;

import static org.nqm.config.GisConfig.currentDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.nqm.GisException;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.model.GisProcessDto;
import org.nqm.utils.GisProcessUtils;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;

public final class Wrapper {

  private Wrapper() {}

  public static final String ORIGIN = "origin";

  private static File getFileMarker() {
    var currentDir = currentDir();
    Future<File> gitModulesFilePath;
    try (var exe = Executors.newVirtualThreadPerTaskExecutor()) {
      gitModulesFilePath = exe.submit(() -> {
        var gitModules = Path.of(currentDir, ".gitmodules").toFile();
        if (gitModules.exists()) {
          return gitModules;
        }
        var gisModules = Path.of(currentDir, ".gis-modules").toFile();
        if (gisModules.exists()) {
          return gisModules;
        }
        return null;
      });
    }
    try {
      return Optional.ofNullable(gitModulesFilePath.get()).orElseThrow(
          () -> new GisException("Could not find '.gis-modules' or '.gitmodules' under this directory!"));
    } catch (InterruptedException | ExecutionException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
  }

  private static void consumeAllModules(Predicate<Path> pred, Function<ExecutorService, Consumer<Path>> f)
      throws IOException {
    var gitModulesFilePath = getFileMarker();
    var currentDir = currentDir();
    try (var exe = Executors.newVirtualThreadPerTaskExecutor()) {
      Optional.of(Path.of(currentDir)).filter(pred).ifPresent(f.apply(exe));
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
          .forEach(f.apply(exe));
    }
  }

  public static Queue<String> forEachModuleDo(String... args) throws IOException {
    return forEachModuleWith(p -> true, args);
  }

  public static Queue<String> forEachModuleWith(Predicate<Path> pred, String... args) throws IOException {
    var output = new ConcurrentLinkedQueue<String>();
    consumeAllModules(pred, exe -> path -> exe.submit(() -> output.add(CommandVerticle.execute(path, args))));
    return output;
  }

  public static void forEachModuleDoRebaseCurrent() throws IOException {
    consumeAllModules(p -> true, exe -> path -> exe.submit(() -> {
      var args = new String[] {"rebase", "%s/%s".formatted(ORIGIN, getCurrentBranchUnderPath(path))};
      CommandVerticle.execute(path, args);
    }));
  }

  public static Queue<String> forEachModuleFetch() throws IOException {
    var output = new ConcurrentLinkedQueue<String>();
    consumeAllModules(p -> true, exe -> path -> exe.submit(() -> {
      CommandVerticle.execute(path, "fetch");
      output.add(CommandVerticle.execute(
          path,
          GitCommand.GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2", "--gis-one-line"));
    }));
    return output;
  }

  public static void forEachModulePruneExcept(String mergedBranch) throws IOException {
    var args = new String[] {
        "for-each-ref",
        "--merged=%s".formatted(mergedBranch),
        "--format=%(refname:short)",
        "refs/heads/",
        "--no-contains",
        mergedBranch
    };
    consumeAllModules(p -> true, exe -> path -> exe.submit(() -> {
      var result = CommandVerticle.executeForDto(path, args).output();
      if (GisStringUtils.isBlank(result)) {
        return;
      }
      Stream.of(result.split(GisStringUtils.NEWLINE))
          .filter(GisStringUtils::isNotBlank)
          .forEach(branch -> CommandVerticle.execute(path, "branch", "-d", branch));
    }));
  }

  public static String getCurrentBranchUnderPath(Path path) {
    GisProcessDto result;
    try {
      result = GisProcessUtils.quickRun(path.toFile(), GisConfig.GIT_HOME_DIR, "branch", "--show-current");
    } catch (IOException e) {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    }
    if (result != null) {
      return result.output().trim();
    }
    return "";
  }
}
