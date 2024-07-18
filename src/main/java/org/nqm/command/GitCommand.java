package org.nqm.command;

import static org.nqm.command.CommandVerticle.GIS_CONCAT_MODULES_NAME_OPT;
import static org.nqm.command.CommandVerticle.GIS_NO_PRINT_MODULES_NAME_OPT;
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
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.nqm.GisException;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.utils.GisProcessUtils;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;
import org.nqm.model.GisSort;
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

  private static void printOutput(Collection<String> output) {
    output.stream().filter(GisStringUtils::isNotBlank).forEach(StdOutUtils::println);
  }

  @Command(name = "pull", aliases = "pu", description = "Fetch from and integrate with remote repositories")
  void pull() throws IOException {
    printOutput(forEachModuleDo("pull"));
  }

  @Command(name = GIT_STATUS, aliases = "st", description = "Show the working trees status")
  void status(
      @Option(names = "--one-line") boolean oneLineOpt,
      @Option(names = "--sort",
          description = "Valid values: ${COMPLETION-CANDIDATES}. "
              + "Default value is 'module_name'. "
              + "Note that the root module will always be on top no matter the sort") GisSort sort)
      throws IOException {
    Queue<String> output;
    if (oneLineOpt) {
      output = forEachModuleDo(GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2", "--gis-one-line");
    } else {
      output = forEachModuleDo(GIT_STATUS, "-sb", "--ignore-submodules", "--porcelain=v2");
    }
    var currentDirName = StdOutUtils.infof("%s", GisStringUtils.getDirName(currentDir()));
    var sorted = output.stream()
        .sorted((a, b) -> sort(oneLineOpt, sort, currentDirName, a, b))
        .toList();
    printOutput(sorted);
    if (Files.exists(TMP_FILE)) {
      var lastFetched = Files.readString(TMP_FILE);
      if (GisStringUtils.isNotBlank(lastFetched)) {
        StdOutUtils.println(FETCHED_AT.formatted(lastFetched));
      }
    }
  }

  private static int sort(boolean oneLineOpt, GisSort sort, String currentDirName, String a, String b) {
    if (a.startsWith(currentDirName)) {
      return Integer.MIN_VALUE;
    }
    if (b.startsWith(currentDirName)) {
      return Integer.MAX_VALUE;
    }
    if (sort == null || GisSort.module_name.equals(sort)) {
      return a.compareTo(b);
    }
    if (GisSort.branch_name.equals(sort)) {
      var branchA = "";
      var branchB = "";
      if (oneLineOpt) {
        branchA = a.split("\s")[1];
        branchB = b.split("\s")[1];
      } else {
        branchA = a.split("\s")[3];
        branchB = b.split("\s")[3];
      }
      return branchA.compareTo(branchB);
    }
    // GisSort.tracking_status == sort:
    var changesA = a.split("\s").length;
    var changesB = b.split("\s").length;
    return changesB - changesA;
  }

  private void fetch() throws IOException {
    forEachModuleDo("fetch");
    var timeFetch = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));

    Files.write(TMP_FILE, timeFetch.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
  }

  @Command(name = "fetch", aliases = "fe", description = "Download objects and refs from other repositories")
  void fetchStatus(@Option(names = "--sort",
      description = "Valid values: ${COMPLETION-CANDIDATES}. "
          + "Default value is 'module_name'. "
          + "Note that the root module will always be on top no matter the sort") GisSort sort)
      throws IOException {
    fetch();
    status(true, sort);
  }

  @Command(name = "rebase-current-origin", aliases = "ru",
      description = "Reapply commits on top of current repositories' origin")
  void rebaseCurrentOrigin() {
    throw new GisException("this function is in progress");
  }

  @Command(name = "rebase-origin", aliases = "re", description = "Reapply commits on top of other base tip")
  void rebaseOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) throws IOException {
    printOutput(forEachModuleDo("rebase", "%s/%s".formatted(ORIGIN, branch)));
  }

  @Command(name = "fetch-origin", aliases = "fo",
      description = "Download objects and refs specified by branch name from other repositories")
  void fetchOrigin(@Parameters(index = "0", paramLabel = "<branch name>") String branch) throws IOException {
    printOutput(forEachModuleDo("fetch", ORIGIN, "%s:%s".formatted(branch, branch)));
  }

  @Command(name = CHECKOUT, aliases = "co", description = "Switch branches or restore working tree files")
  void checkout(@Parameters(index = "0", paramLabel = "<branch name>") String branch) throws IOException {
    printOutput(forEachModuleDo(CHECKOUT, branch));
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
      printOutput(forEachModuleDo(CHECKOUT, "-b", newBranch));
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
    printOutput(forEachModuleWith(specifiedPaths::contains, CHECKOUT, "-b", newBranch));
  }

  @Command(name = "remove-branch", aliases = "rm",
      description = "Delete specified branch from all repositories")
  void removeBranch(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
      @Option(names = "-f",
          description = "force to delete branch without interactive prompt") boolean isForce)
      throws IOException {
    if (isForce || isConfirmed("Sure you want to remove branch '%s' ? [Y/n]".formatted(branch))) {
      printOutput(forEachModuleDo("branch", "-d", branch));
    }
  }

  @Command(name = "push", aliases = "pus", description = "Update remotes refs along with associated objects")
  void push(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
      @Option(names = "-f", description = "force to update remote origin branch") boolean force,
      @Option(names = "-r", description = "push to remote origin branch") boolean newRemoteBranch,
      @Option(names = "--no-interactive", description = "do not prompt for user input") boolean noInteractive)
      throws IOException {

    if (!noInteractive && !isConfirmed("Sure you want to push to remote '%s' [Y/n]".formatted(branch))) {
      return;
    }
    var args = newRemoteBranch
        ? new String[] {"push", "-u", ORIGIN, branch}
        : shouldForcePush(force);

    printOutput(forEachModuleWith(path -> isSameBranchUnderPath(branch, path), args));
  }

  @Command(name = "remote-prune-origin", aliases = "rpo",
      description = "Deletes stale references associated with <branch>")
  void remotePruneOrigin() throws IOException {
    printOutput(forEachModuleDo("remote", "prune", ORIGIN));
  }

  @Command(name = "local-prune", aliases = "prune",
      description = "Deletes stale local references which already merged to <branch>")
  void localPrune(@Parameters(index = "0", paramLabel = "<default branch name>") String branch)
      throws IOException {
    // TODO: rework this function to remove HOOKS
    printOutput(forEachModuleDo("for-each-ref",
        "--merged=%s".formatted(branch),
        "--format=%(refname:short)",
        "refs/heads/",
        "--no-contains",
        branch,
        HOOKS_OPTION,
        GisConfig.GIT_HOME_DIR + " branch -d %s"));
  }

  @Command(name = "stash", description = "Stash the changes in a dirty working directories away")
  void stash(@Option(names = "--pop", description = "pop first stashed changes") boolean isPop)
      throws IOException {
    var args = isPop ? new String[] {"stash", "pop"} : new String[] {"stash"};
    printOutput(forEachModuleDo(args));
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
      sArgs = Stream.concat(sArgs, Stream.of(GIS_NO_PRINT_MODULES_NAME_OPT));
    }
    final var args = sArgs.toArray(String[]::new);
    printOutput(forEachModuleDo(args));
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
    printOutput(forEachModuleDo("diff", "--name-only", GIS_CONCAT_MODULES_NAME_OPT));
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
