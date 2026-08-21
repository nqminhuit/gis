package org.nqm.command;

import static org.nqm.config.GisConfig.currentDir;
import org.nqm.GisException;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.model.GisProcessDto;
import org.nqm.utils.GisProcessUtils;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

  public static Queue<String> forEachModuleDo(String... args) throws IOException {
    return forEachModuleWith(p -> true, args);
  }

  public static Queue<String> forEachModuleWith(Predicate<Path> pred, String... args) throws IOException {
    return runOnModules(pred, path -> CommandVerticle.execute(path, args));
  }

  private record ModuleTask(Path path, Future<?> future) {}

  private static Queue<String> runOnModules(Predicate<Path> pred, Function<Path, String> action)
      throws IOException {
    var output = new ConcurrentLinkedQueue<String>();
    var tasks = new ArrayList<ModuleTask>();
    var gitModulesFilePath = getFileMarker();
    var currentDir = currentDir();
    try (var exe = Executors.newVirtualThreadPerTaskExecutor()) {
      Optional.of(Path.of(currentDir)).filter(pred).ifPresent(path ->
          tasks.add(new ModuleTask(path, exe.submit(() -> output.add(action.apply(path))))));

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
          .forEach(path -> tasks.add(new ModuleTask(path, exe.submit(() -> output.add(action.apply(path))))));

      // Wait for all futures with configured timeout
      long timeoutSeconds = org.nqm.config.GisConfig.getModuleTimeoutSeconds();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
      for (var task : tasks) {
        long remaining = deadline - System.nanoTime();
        try {
          task.future().get(Math.max(remaining, 0), TimeUnit.NANOSECONDS);
        } catch (java.util.concurrent.TimeoutException te) {
          StdOutUtils.warnln(
              "module execution timed out after %ds, unfinished modules are aborted!".formatted(timeoutSeconds));
          cancelUnfinished(tasks);
          break;
        } catch (InterruptedException ie) {
          GisLog.debug(ie);
          cancelUnfinished(tasks);
          Thread.currentThread().interrupt();
          break;
        } catch (ExecutionException ee) {
          GisLog.debug(ee);
          var cause = ee.getCause() == null ? ee : ee.getCause();
          StdOutUtils.errln("module '%s' failed: %s".formatted(task.path().getFileName(), cause.getMessage()));
        }
      }
    }
    return output;
  }

  private static void cancelUnfinished(Iterable<ModuleTask> tasks) {
    for (var task : tasks) {
      if (!task.future().isDone()) {
        task.future().cancel(true);
      }
    }
  }

  public static void forEachModuleDoRebaseCurrent() throws IOException {
    runOnModules(p -> true, path ->
        CommandVerticle.execute(path, "rebase", "%s/%s".formatted(ORIGIN, getCurrentBranchUnderPath(path))));
  }

  public static Queue<String> forEachModuleFetch() throws IOException {
    return runOnModules(p -> true, path -> {
      CommandVerticle.execute(path, "fetch");
      return CommandVerticle.execute(
          path,
          GitCommand.GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v1", "--gis-one-line");
    });
  }

  public static void forEachModuleFetchInBackground() throws IOException {
    runOnModules(p -> true, path -> {
      CommandVerticle.executeInBackground(path, "fetch");
      return "";
    });
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
    runOnModules(p -> true, path -> {
      var result = CommandVerticle.executeForDto(path, args).output();
      if (GisStringUtils.isBlank(result)) {
        return "";
      }
      Stream.of(result.split(GisStringUtils.NEWLINE))
          .filter(GisStringUtils::isNotBlank)
          .forEach(branch -> CommandVerticle.execute(path, "branch", "-d", branch));
      return "";
    });
  }

  public static String getCurrentBranchUnderPath(Path path) {
    GisProcessDto result;
    try {
      result = GisProcessUtils.quickRun(path.toFile(), GisConfig.GIT_HOME_DIR, "branch", "--show-current");
    }
    catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
    catch (IOException e) {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    }
    if (result != null) {
      return result.output().trim();
    }
    return "";
  }
}
