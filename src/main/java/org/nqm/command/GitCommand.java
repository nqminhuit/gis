package org.nqm.command;

import static java.lang.System.out; // NOSONAR
import static org.nqm.command.Wrapper.deployVertx;
import static org.nqm.command.Wrapper.forEachModuleDo;
import static org.nqm.command.Wrapper.forEachModuleWith;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class GitCommand {

  private static final String ORIGIN = "origin";

  public static final String GIT_STATUS = "status";
  public static final String HOOKS_OPTION = "--hooks";

  @Command(name = "pull", aliases = "pu")
  void pull() {
    forEachModuleDo(path -> deployVertx(path, "pull"));
  }

  @Command(name = GIT_STATUS, aliases = "st")
  void status(@Option(names = "--one-line") boolean oneLineOpt) {
    if (oneLineOpt) {
      forEachModuleDo(path -> deployVertx(path, GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2", "--gis-one-line"));
      return;
    }
    forEachModuleDo(path -> deployVertx(path, GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2"));
  }

  @Command(name = "fetch", aliases = "fe")
  void fetch() {
    forEachModuleDo(path -> deployVertx(path, "fetch"));
  }

  @Command(name = "fetch-origin", aliases = "fo")
  void fetchOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    forEachModuleDo(path -> deployVertx(path, "fetch", ORIGIN, "%s:%s".formatted(branch, branch)));
  }

  @Command(name = "checkout", aliases = "co")
  void checkout(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    forEachModuleDo(path -> deployVertx(path, "checkout", branch));
  }

  @Command(name = "checkout-branch",
    aliases = "cb",
    description = "Create (and checkout) a spin-off branch with <new_branch_name> for <modules>.")
  void checkoutNewBranch(
    @Parameters(index = "0", paramLabel = "<new_branch_name>", description = "branch name") String newBranch,
    @Parameters(paramLabel = "<modules>",
      description = "Specified modules. If empty, will create for all submodules and root.") String... modules) {

    Consumer<Path> deployCommand = path -> deployVertx(path, "checkout", "-b", newBranch);

    if (null == modules) {
      forEachModuleDo(deployCommand);
      return;
    }
    streamOf(modules)
      .distinct()
      .filter(Predicate.not(String::isBlank))
      .filter(module -> Path.of(module).toFile().exists())
      .map(Path::of)
      .forEach(deployCommand);
  }

  @Command(name = "remove-branch", aliases = "rm")
  void removeBranch(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    if (isConfirmed("Sure you want to remove branch '%s' ? [Y/n]".formatted(branch))) {
      forEachModuleDo(path -> deployVertx(path, "branch", "-d", branch));
    }
  }

  @Command(name = "push", aliases = "pus")
  void push(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
    @Option(names = "-f", description = "force to update remote origin branch") boolean isForce,
    @Option(names = "-r", description = "push to remote origin branch") boolean isNewRemoteBranch) {

    if (!isConfirmed("Sure you want to push to remote '%s' [Y/n]".formatted(branch))) {
      return;
    }
    var args = isNewRemoteBranch ? new String[] { "push", "-u", ORIGIN, branch } : shouldForcePush(isForce);
    forEachModuleWith(path -> isSameBranchUnderPath(branch, path), path -> deployVertx(path, args));
  }

  @Command(name = "remote-prune-origin", aliases = "rpo")
  void remotePruneOrigin() {
    forEachModuleDo(path -> deployVertx(path, "remote", "prune", ORIGIN));
  }

  @Command(name = "local-prune", aliases = "prune")
  void localPrune(@Parameters(index = "0", paramLabel = "<default branch name>") String branch) {
    forEachModuleDo(path -> deployVertx(path,
      "for-each-ref",
      "--merged=%s".formatted(branch),
      "--format=%(refname:short)",
      "refs/heads/",
      "--no-contains",
      branch,
      HOOKS_OPTION,
      GisConfig.GIT_HOME_DIR + " branch -d %s"));
  }

  @Command(name = "stash")
  void stash(@Option(names = "-pp", description = "pop first stashed changes") boolean isPop) {
    var args = isPop ? new String[] { "stash", "pop" } : new String[] { "stash" };
    forEachModuleDo(path -> deployVertx(path, args));
  }

  @Command(name = "branches")
  void listBranches(
    @Option(names = "-nn", description = "do not print module name") boolean noPrintModuleName) {
    var sArgs = Stream.of("for-each-ref", "--format=%(refname:short)", "refs/heads/" );
    if (noPrintModuleName) {
      sArgs = Stream.concat(sArgs, Stream.of("--gis-no-print-modules-name" ));
    }
    final var args = sArgs.toArray(String[]::new);
    forEachModuleDo(path -> deployVertx(path, args));
  }

  @Command(name = "init", description = "init .gis-modules for current directory")
  void init() throws IOException {
    var data = Files.list(Path.of("."))
        .filter(Files::isDirectory)
        .map(p -> p.getFileName())
        .map("path = %s"::formatted)
        .collect(Collectors.joining("\n"))
        .getBytes();
    Files.write(Paths.get(".gis-modules"), data);
  }

  @Command(name = "files", description = "show modified files of all submodules")
  void files() {
    forEachModuleDo(path -> deployVertx(path, "diff", "--name-only", "--gis-concat-modules-name"));
  }

  private static Stream<String> streamOf(String[] input) {
    return Stream.of(input).map(String::trim).distinct();
  }

  private boolean isSameBranchUnderPath(String branch, Path path) {
    try (BufferedReader currentBranch = new BufferedReader(
        new InputStreamReader(new ProcessBuilder(GisConfig.GIT_HOME_DIR, "branch", "--show-current")
            .directory(path.toFile())
            .start()
            .getInputStream()))) {
      return currentBranch.readLine().equals(branch);
    } catch (IOException e) {
      GisLog.debug(e);
      return false;
    }
  }

  private boolean isConfirmed(String question) {
    out.print(question + " ");
    try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
      var input = reader.readLine();
      return Stream.of("y", "ye", "yes").anyMatch(s -> s.equalsIgnoreCase(input));
    }
    catch (IOException e) {
      GisLog.debug(e);
      return false;
    }
  }

  private String[] shouldForcePush(boolean isForce) {
    return isForce ? new String[] { "push", "-f" } : new String[] { "push" };
  }
}
