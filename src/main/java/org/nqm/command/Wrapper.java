package org.nqm.command;

import static org.nqm.config.GisConfig.currentDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  public static List<String> forEachModuleDo(String... args) throws IOException {
    return forEachModuleWith(p -> true, args);
  }

  public static List<String> forEachModuleWith(Predicate<Path> pred, String... args) throws IOException {
    var output = Collections.synchronizedList(new ArrayList<String>());
    consumeAllModules(pred, exe -> path -> exe.submit(() -> output.add(CommandVerticle.execute(path, args))));
    return output;
  }

  public static void forEachModuleDoRebaseCurrent() throws IOException {
    consumeAllModules(p -> true, exe -> path -> exe.submit(() -> {
      var args = new String[] {"rebase", "%s/%s".formatted(ORIGIN, getCurrentBranchUnderPath(path))};
      CommandVerticle.execute(path, args);
    }));
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
