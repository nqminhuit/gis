package org.nqm.command;

import static java.lang.System.out;
import static org.nqm.command.Wrapper.deployVertx;
import static org.nqm.command.Wrapper.forEachModuleDo;
import static org.nqm.command.Wrapper.forEachModuleWith;
import static org.nqm.command.Wrapper.forEachSubmoduleDo;
import static org.nqm.utils.GisStringUtils.convertToPathFromRegex;
import static org.nqm.utils.StdOutUtils.errln;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.utils.GisStringUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class GitCommand {

  private static final String ALL_MODULES = "***";
  private static final String ALL_SUBMODULES = "/**";

  @Command(name = "pull", aliases = "pu")
  void pull() {
    forEachModuleDo(path -> deployVertx(path, "pull"));
  }

  @Command(name = "status", aliases = "st")
  void status() {
    forEachModuleDo(path -> deployVertx(path, "status", "-sb", "--ignore-submodules", "--porcelain=v2"));
  }

  @Command(name = "fetch", aliases = "fe")
  void fetch() {
    forEachModuleDo(path -> deployVertx(path, "fetch"));
  }

  @Command(name = "checkout", aliases = "co")
  void checkout(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    forEachModuleDo(path -> deployVertx(path, "checkout", branch));
  }

  @Command(name = "checkout-branch", aliases = "cb")
  void checkoutNewBranch(@Parameters(index = "0", paramLabel = "<new_branch_name>") String newBranch) {
    out.println("""
      Which module to create branch?
      (separate by ',', use '.' for root, use '%s' for all modules, use '%s' for all submodules)
      """.formatted(ALL_MODULES, ALL_SUBMODULES));
    var paths = new ArrayList<String>();
    forEachModuleDo(path -> {
      var sPath = path.toString();
      out.println(sPath.equals(GisConfig.CURRENT_DIR) ? " - ./" : " - " + path.getFileName());
      paths.add(sPath);
    });

    try (var inputRepos = new BufferedReader(new InputStreamReader(System.in))) {
      var input = inputRepos.readLine().split(",");
      if (input.length == 1 && GisStringUtils.isBlank(input[0])) {
        System.exit(0); // TODO: should centralize system exit to 1 place?
      }
      Consumer<Path> deployCommand = path -> deployVertx(path, "checkout", "-b", newBranch);
      if (streamOf(input).filter(ALL_MODULES::equals).findFirst().isPresent()) {
        forEachModuleDo(deployCommand);
        return;
      }

      if (streamOf(input).filter(ALL_SUBMODULES::equals).findFirst().isPresent()) {
        forEachSubmoduleDo(deployCommand);
        return;
      }

      streamOf(input)
        .map(module -> convertToPathFromRegex(module, paths))
        .filter(Predicate.not(String::isBlank))
        .map(Path::of)
        .forEach(deployCommand);
    }
    catch (IOException e) {
      errln(e.getMessage());
      GisLog.debug(e);
    }
  }

  private static Stream<String> streamOf(String[] input) throws IOException {
    return Stream.of(input).map(String::trim).distinct();
  }

  @Command(name = "remove-branch", aliases = "rm")
  void removeBranch(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    if (isConfirmed("Are you sure you want to remove branch '%s' ? [Y/n]".formatted(branch))) {
      forEachModuleDo(path -> deployVertx(path, "branch", "-d", branch));
    }
  }

  @Command(name = "push", aliases = "pus")
  void push(@Parameters(index = "0", paramLabel = "<branch name>") String branch,
    @Option(names = "-r", description = "push to remote origin branch") boolean newRemoteBranch) {

    if (!isConfirmed("Are you sure you want to push to remote '%s' [Y/n]".formatted(branch))) {
      return;
    }
    var args = newRemoteBranch ? new String[] { "push", "-u", "origin", branch } : new String[] { "push" };
    forEachModuleWith(
      path -> isSameBranchUnderPath(branch, path),
      path -> deployVertx(path, args));
  }

  private boolean isSameBranchUnderPath(String branch, Path path) {
    try {
      var proc = new ProcessBuilder("git", "branch", "--show-current")
        .directory(path.toFile())
        .start();
      var currentBranch = new BufferedReader(new InputStreamReader(proc.getInputStream())).readLine();
      return currentBranch.equals(branch);
    }
    catch (IOException e) {
      GisLog.debug(e);
      return false;
    }
  }

  private boolean isConfirmed(String question) {
    out.print(question + " ");
    try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
      var input = reader.readLine();
      return Stream.of(new String[] { "y", "ye", "yes" })
        .filter(s -> s.equalsIgnoreCase(input))
        .findFirst()
        .isPresent();
    }
    catch (IOException e) {
      GisLog.debug(e);
      return false;
    }
  }

}
