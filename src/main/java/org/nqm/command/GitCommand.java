package org.nqm.command;

import static org.nqm.command.Wrapper.deployVertx;
import static org.nqm.command.Wrapper.execute;
import static org.nqm.utils.GisStringUtils.convertToPathFromRegex;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

public class GitCommand {

  @Command(name = "pull", aliases = "pu")
  void pull() {
    execute(path -> deployVertx(path, "pull"));
  }

  @Command(name = "status", aliases = "st")
  void status() {
    execute(path -> deployVertx(path, true, "status", "-sb", "--ignore-submodules"));
  }

  @Command(name = "fetch", aliases = "fe")
  void fetch() {
    execute(path -> deployVertx(path, "fetch"));
  }

  @Command(name = "checkout", aliases = "co")
  void checkout(@Parameters(index = "0", paramLabel = "<branch name>") String branch) {
    execute(path -> deployVertx(path, "checkout", branch));
  }

  @Command(name = "create-branch", aliases = "cb")
  void checkoutNewBranch(@Parameters(index = "0", paramLabel = "<new_branch_name>") String newBranch) {
    System.out.println("Which repositories to create branch? (separate by comma, use '.' for root)");
    var paths = new ArrayList<String>();
    Wrapper.getAllModuleDirs(path -> {
      System.out.println(" - " + path);
      paths.add(path.toString());
    });

    var inputRepos = new BufferedReader(new InputStreamReader(System.in));
    try {
      Stream.of(inputRepos.readLine().split(","))
        .map(regex -> convertToPathFromRegex(regex, paths))
        .filter(Predicate.not(String::isBlank))
        .forEach(repo -> {
          System.out.println("running git checkout -b '%s' in repo '%s'".formatted(newBranch, repo));
        });
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.exit(0);
  }

}
