package org.nqm.command;

import static org.nqm.command.Wrapper.forEachModuleDo;
import static org.nqm.command.Wrapper.forEachModuleWith;
import static org.nqm.config.GisConfig.currentDir;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.nqm.GisException;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.utils.GisProcessUtils;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class GitCommand {

  private static final String CHECKOUT = "checkout";
  private static final String FETCHED_AT = "(fetched at: %s)";
  private static final String ORIGIN = "origin";
  private static final Path TMP_FILE = Path.of("/", "tmp", "gis_fetch" + currentDir().replace("/", "_"));

  static final String GIS_AUTOCOMPLETE_FILE = "_gis";
  static final Pattern CONFIRM_YES = Pattern.compile("[Yy]+([Ee][Ss])*");

  public static final String GIT_STATUS = "status";
  public static final String HOOKS_OPTION = "--hooks";

  @Command(name = "pull", aliases = "pu", description = "Fetch from and integrate with remote repositories")
  void pull() throws IOException {
    forEachModuleDo("pull");
  }

  @Command(name = GIT_STATUS, aliases = "st", description = "Show the working trees status")
  void status(@Option(names = "--one-line") boolean oneLineOpt) throws IOException {
    if (oneLineOpt) {
      forEachModuleDo(GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2", "--gis-one-line");
    } else {
      forEachModuleDo(GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2");
    }
    if (Files.exists(TMP_FILE)) {
      var lastFetched = Files.readString(TMP_FILE);
      if (GisStringUtils.isNotBlank(lastFetched)) {
        StdOutUtils.println(FETCHED_AT.formatted(lastFetched));
      }
    }
  }

  private void fetch() throws IOException {
    forEachModuleDo("fetch");
    var timeFetch = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));

    Files.write(TMP_FILE, timeFetch.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    StdOutUtils.println(FETCHED_AT.formatted(timeFetch));
  }

  @Command(name = "fetch", aliases = "fe", description = "Download objects and refs from other repositories")
  void fetchStatus() throws IOException {
    try {
      StdOutUtils.setMuteOutput(true);
      fetch();
    } finally {
      StdOutUtils.setMuteOutput(false);
    }
    status(true);
  }

  @Command(name = "rebase-current-origin", aliases = "ru",
      description = "Reapply commits on top of current repositories' origin")
  void rebaseCurrentOrigin() {
    throw new GisException("this function is in progress");
  }

  @Command(name = "rebase-origin", aliases = "re", description = "Reapply commits on top of other base tip")
  void rebaseOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) throws IOException {
    forEachModuleDo("rebase", "%s/%s".formatted(ORIGIN, branch));
  }

  @Command(name = "fetch-origin", aliases = "fo",
      description = "Download objects and refs specified by branch name from other repositories")
  void fetchOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) throws IOException {
    forEachModuleDo("fetch", ORIGIN, "%s:%s".formatted(branch, branch));
  }

  @Command(name = CHECKOUT, aliases = "co", description = "Switch branches or restore working tree files")
  void checkout(@Parameters(index = "0", paramLabel = "<branch name>") String branch) throws IOException {
    forEachModuleDo(CHECKOUT, branch);
  }

  @Command(name = "spin-off",
      aliases = "cb",
      description = "Create (and checkout) a spin-off branch with <new_branch_name> for <modules>.")
  void spinOff(
      @Parameters(index = "0", paramLabel = "<new_branch_name>",
          description = "branch name") String newBranch,
      @Parameters(paramLabel = "<modules>",
          description = "Specified modules. If empty, will create for all submodules and root.") String... modules)
      throws IOException {
    if (null == modules || modules.length < 1) {
      forEachModuleDo(CHECKOUT, "-b", newBranch);
      return;
    }
    var currentDir = currentDir();
    var currentPath = Optional.of(Path.of(currentDir))
        .map(p -> p.subpath(p.getNameCount() - 1, p.getNameCount()))
        .map(Path::toString)
        .orElse(null);
    var specifiedPaths = streamOf(modules)
        .filter(Predicate.not(String::isBlank))
        .map(module -> {
          if (module.equals(currentPath)) {
            return Path.of(currentDir);
          }
          return Path.of(currentDir, module);
        })
        .filter(p -> p.toFile().exists())
        .toList();
    forEachModuleWith(specifiedPaths::contains, CHECKOUT, "-b", newBranch);
  }

  @Command(name = "remove-branch", aliases = "rm",
      description = "Delete specified branch from all repositories")
  void removeBranch(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
      @Option(names = "-f",
          description = "force to delete branch without interactive prompt") boolean isForce)
      throws IOException {
    if (isForce || isConfirmed("Sure you want to remove branch '%s' ? [Y/n]".formatted(branch))) {
      forEachModuleDo("branch", "-d", branch);
    }
  }

  @Command(name = "push", aliases = "pus", description = "Update remotes refs along with associated objects")
  void push(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
      @Option(names = "-f", description = "force to update remote origin branch") boolean isForce,
      @Option(names = "-r", description = "push to remote origin branch") boolean isNewRemoteBranch)
      throws IOException {

    if (!isConfirmed("Sure you want to push to remote '%s' [Y/n]".formatted(branch))) {
      return;
    }
    var args = isNewRemoteBranch
        ? new String[] {"push", "-u", ORIGIN, branch}
        : shouldForcePush(isForce);

    forEachModuleWith(path -> isSameBranchUnderPath(branch, path), args);
  }

  @Command(name = "remote-prune-origin", aliases = "rpo",
      description = "Deletes stale references associated with <branch>")
  void remotePruneOrigin() throws IOException {
    forEachModuleDo("remote", "prune", ORIGIN);
  }

  @Command(name = "local-prune", aliases = "prune",
      description = "Deletes stale local references which already merged to <branch>")
  void localPrune(@Parameters(index = "0", paramLabel = "<default branch name>") String branch)
      throws IOException {
    forEachModuleDo("for-each-ref",
        "--merged=%s".formatted(branch),
        "--format=%(refname:short)",
        "refs/heads/",
        "--no-contains",
        branch,
        HOOKS_OPTION,
        GisConfig.GIT_HOME_DIR + " branch -d %s");
  }

  @Command(name = "stash", description = "Stash the changes in a dirty working directories away")
  void stash(@Option(names = "--pop", description = "pop first stashed changes") boolean isPop)
      throws IOException {
    var args = isPop ? new String[] {"stash", "pop"} : new String[] {"stash"};
    forEachModuleDo(args);
  }

  @Command(name = "branches", description = "List branches from all submodules")
  void listBranches(
      @Option(names = "--no-module-name", description = "do not print module name") boolean noPrintModuleName,
      @Option(names = "--include-remotes", description = "include remote branches") boolean includeRemotes)
      throws IOException {
    var sArgs = Stream.of("for-each-ref", "--format=%(refname:short)", "refs/heads");
    if (includeRemotes) {
      sArgs = Stream.concat(sArgs, Stream.of("refs/remotes"));
    }
    if (noPrintModuleName) {
      sArgs = Stream.concat(sArgs, Stream.of("--gis-no-print-modules-name"));
    }
    final var args = sArgs.toArray(String[]::new);
    forEachModuleDo(args);
  }

  @Command(name = "init", description = "init .gis-modules for current directory")
  void init() throws IOException {
    var currentDir = Path.of(currentDir());
    try (var stream = Files.list(currentDir)) {
      var lines = stream.filter(Files::isDirectory)
          .map(Path::getFileName)
          .map("path = %s"::formatted)
          .toList();
      Files.write(currentDir.resolve(".gis-modules"), lines);
    }
  }

  @Command(name = "files", description = "List all modified files from submodules")
  void files() throws IOException {
    forEachModuleDo("diff", "--name-only", "--gis-concat-modules-name");
  }

  @Command(name = "completion", description = "Generate an zsh auto completion script")
  void generateCompletion(
      @Option(names = "--directory",
          description = "export completion zsh function to file at specified directory") Path dir)
      throws IOException {
    try (var stream = this.getClass().getClassLoader().getResourceAsStream(GIS_AUTOCOMPLETE_FILE)) {
      var buffer = new BufferedReader(new InputStreamReader(stream));
      String line = null;
      if (dir != null) {
        var file = dir.resolve(GIS_AUTOCOMPLETE_FILE);
        try (var out = new FileOutputStream(file.toFile())) {
          while ((line = buffer.readLine()) != null) {
            out.write(line.getBytes());
            out.write("%n".formatted().getBytes());
          }
        }
        return;
      }
      while ((line = buffer.readLine()) != null) {
        StdOutUtils.println(line);
      }
    }
  }

  private static Stream<String> streamOf(String[] input) {
    return Stream.of(input).map(String::trim).distinct();
  }

  private String getCurrentBranchUnderPath(Path path) throws IOException {
    var result = GisProcessUtils.quickRun(path.toFile(), GisConfig.GIT_HOME_DIR, "branch", "--show-current");
    return result.output().trim();
  }

  private boolean isSameBranchUnderPath(String branch, Path path) {
    if (GisStringUtils.isBlank(branch)) {
      return false;
    }
    try {
      return branch.equals(getCurrentBranchUnderPath(path));
    } catch (IOException e) {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    }
  }

  private boolean isConfirmed(String question) throws IOException {
    StdOutUtils.print(question + " ");
    try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
      return CONFIRM_YES.matcher(reader.readLine()).matches();
    }
  }

  private String[] shouldForcePush(boolean isForce) {
    return isForce ? new String[] {"push", "-f"} : new String[] {"push"};
  }
}
