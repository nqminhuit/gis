package org.nqm.command;

import static org.nqm.command.Wrapper.forEachModuleDo;
import static org.nqm.command.Wrapper.forEachModuleWith;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.exception.GisException;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class GitCommand {

  private static final String CHECKOUT = "checkout";
  private static final String FETCHED_AT = "(fetched at: %s)";
  private static final String ORIGIN = "origin";
  private static final Path TMP_FILE =
      Path.of("/", "tmp", "gis_fetch" + GisConfig.CURRENT_DIR.replace("/", "_"));

  public static final String GIT_STATUS = "status";
  public static final String HOOKS_OPTION = "--hooks";

  @Command(name = "pull", aliases = "pu")
  void pull() {
    forEachModuleDo("pull");
  }

  @Command(name = GIT_STATUS, aliases = "st")
  void status(@Option(names = "--one-line") boolean oneLineOpt) {
    if (oneLineOpt) {
      forEachModuleDo(GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2", "--gis-one-line");
    } else {
      forEachModuleDo(GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2");
    }
    var lastFetched = safelyReadLastFetched(TMP_FILE);
    if (GisStringUtils.isNotBlank(lastFetched)) {
      StdOutUtils.println(FETCHED_AT.formatted(lastFetched));
    }
  }

  @Command(name = "fetch", aliases = "fe")
  void fetch() {
    forEachModuleDo("fetch");
    var timeFetch = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));

    try {
      Files.write(TMP_FILE, timeFetch.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    } catch (IOException e) {
      GisLog.debug(e);
      return;
    }

    StdOutUtils.println(FETCHED_AT.formatted(timeFetch));
  }

  @Command(name = "rebase-current-origin", aliases = "ru")
  void rebaseCurrentOrigin() {
    throw new GisException("this function is in progress");
  }

  @Command(name = "rebase-origin", aliases = "re")
  void rebaseOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    forEachModuleDo("rebase", "%s/%s".formatted(ORIGIN, branch));
  }

  @Command(name = "fetch-origin", aliases = "fo")
  void fetchOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    forEachModuleDo("fetch", ORIGIN, "%s:%s".formatted(branch, branch));
  }

  @Command(name = CHECKOUT, aliases = "co")
  void checkout(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    forEachModuleDo(CHECKOUT, branch);
  }

  @Command(name = "checkout-branch",
      aliases = "cb",
      description = "Create (and checkout) a spin-off branch with <new_branch_name> for <modules>.")
  void checkoutNewBranch(
      @Parameters(index = "0", paramLabel = "<new_branch_name>",
          description = "branch name") String newBranch,
      @Parameters(paramLabel = "<modules>",
          description = "Specified modules. If empty, will create for all submodules and root.") String... modules) {
    if (null == modules) {
      forEachModuleDo(CHECKOUT, "-b", newBranch);
      return;
    }
    var specifiedPaths = streamOf(modules)
        .distinct()
        .filter(Predicate.not(String::isBlank))
        .filter(module -> Path.of(module).toFile().exists())
        .map(Path::of)
        .toList();
    forEachModuleWith(specifiedPaths::contains, CHECKOUT, "-b", newBranch);
  }

  @Command(name = "remove-branch", aliases = "rm")
  void removeBranch(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    if (isConfirmed("Sure you want to remove branch '%s' ? [Y/n]".formatted(branch))) {
      forEachModuleDo("branch", "-d", branch);
    }
  }

  @Command(name = "push", aliases = "pus")
  void push(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
      @Option(names = "-f", description = "force to update remote origin branch") boolean isForce,
      @Option(names = "-r", description = "push to remote origin branch") boolean isNewRemoteBranch) {

    if (!isConfirmed("Sure you want to push to remote '%s' [Y/n]".formatted(branch))) {
      return;
    }
    var args = isNewRemoteBranch ? new String[] {"push", "-u", ORIGIN, branch} : shouldForcePush(isForce);
    forEachModuleWith(path -> isSameBranchUnderPath(branch, path), args);
  }

  @Command(name = "remote-prune-origin", aliases = "rpo")
  void remotePruneOrigin() {
    forEachModuleDo("remote", "prune", ORIGIN);
  }

  @Command(name = "local-prune", aliases = "prune")
  void localPrune(@Parameters(index = "0", paramLabel = "<default branch name>") String branch) {
    forEachModuleDo("for-each-ref",
        "--merged=%s".formatted(branch),
        "--format=%(refname:short)",
        "refs/heads/",
        "--no-contains",
        branch,
        HOOKS_OPTION,
        GisConfig.GIT_HOME_DIR + " branch -d %s");
  }

  @Command(name = "stash")
  void stash(@Option(names = "-pp", description = "pop first stashed changes") boolean isPop) {
    var args = isPop ? new String[] {"stash", "pop"} : new String[] {"stash"};
    forEachModuleDo(args);
  }

  @Command(name = "branches")
  void listBranches(
      @Option(names = "-nn", description = "do not print module name") boolean noPrintModuleName) {
    var sArgs = Stream.of("for-each-ref", "--format=%(refname:short)", "refs/heads/");
    if (noPrintModuleName) {
      sArgs = Stream.concat(sArgs, Stream.of("--gis-no-print-modules-name"));
    }
    final var args = sArgs.toArray(String[]::new);
    forEachModuleDo(args);
  }

  @Command(name = "init", description = "init .gis-modules for current directory")
  void init() {
    try (var stream = Files.list(Path.of("."))) {
      var data = stream.filter(Files::isDirectory)
          .map(Path::getFileName)
          .map("path = %s"::formatted)
          .collect(Collectors.joining("\n"))
          .getBytes();
      Files.write(Paths.get(".gis-modules"), data);
    } catch (IOException e) {
      GisLog.debug(e);
      StdOutUtils.errln(e.getMessage());
    }
  }

  @Command(name = "files", description = "show modified files of all submodules")
  void files() {
    forEachModuleDo("diff", "--name-only", "--gis-concat-modules-name");
  }

  private static Stream<String> streamOf(String[] input) {
    return Stream.of(input).map(String::trim).distinct();
  }

  private String getCurrentBranchUnderPath(Path path) {
    try (BufferedReader currentBranch = new BufferedReader(
        new InputStreamReader(new ProcessBuilder(GisConfig.GIT_HOME_DIR, "branch", "--show-current")
            .directory(path.toFile())
            .start()
            .getInputStream()))) {
      return currentBranch.readLine();
    } catch (IOException e) {
      GisLog.debug(e);
      return "";
    }
  }

  private boolean isSameBranchUnderPath(String branch, Path path) {
    if (GisStringUtils.isBlank(branch)) {
      return false;
    }
    return branch.equals(getCurrentBranchUnderPath(path));
  }

  private boolean isConfirmed(String question) {
    StdOutUtils.print(question + " ");
    try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
      var input = reader.readLine();
      return Stream.of("y", "ye", "yes").anyMatch(s -> s.equalsIgnoreCase(input));
    } catch (IOException e) {
      GisLog.debug(e);
      return false;
    }
  }

  private String[] shouldForcePush(boolean isForce) {
    return isForce ? new String[] {"push", "-f"} : new String[] {"push"};
  }

  private static String safelyReadLastFetched(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      GisLog.debug(e);
      return "";
    }
  }
}
