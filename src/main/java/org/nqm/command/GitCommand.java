package org.nqm.command;

import static java.lang.System.out;
import static org.nqm.command.Wrapper.deployVertx;
import static org.nqm.command.Wrapper.forEachModuleDo;
import static org.nqm.utils.GisStringUtils.convertToPathFromRegex;
import static org.nqm.utils.StdOutUtils.errln;
import org.nqm.config.GisConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

public class GitCommand {

  @Command(name = "pull", aliases = "pu")
  void pull() {
    forEachModuleDo(path -> deployVertx(path, "pull"));
  }

  @Command(name = "status", aliases = "st")
  void status() {
    forEachModuleDo(path -> deployVertx(path, true, "status", "-sb", "--ignore-submodules"));
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
    out.println("Which repositories to create branch? (separate by comma, use '.' for root)");
    var paths = new ArrayList<String>();
    forEachModuleDo(path -> {
      var sPath = path.toString();
      out.println(sPath.equals(GisConfig.CURRENT_DIR) ? " - ./" : " - " + path.getFileName());
      paths.add(sPath);
    });

    try (var inputRepos = new BufferedReader(new InputStreamReader(System.in))) {
      Stream.of(inputRepos.readLine().split(","))
        .distinct()
        .map(regex -> convertToPathFromRegex(regex.trim(), paths))
        .filter(Predicate.not(String::isBlank))
        .forEach(repo -> deployVertx(Path.of(repo), "checkout", "-b", newBranch));
    }
    catch (IOException e) {
      // TODO log stacktrace if enable debug.
      errln(e.getMessage());
    }
  }

  @Command(name = "remove-branch", aliases = "rm")
  void removeBranch(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    // TODO need confirmation/acknowledgement from user
    forEachModuleDo(path -> deployVertx(path, "branch", "-d", branch));
  }

}
