package org.nqm.vertx;

import static java.lang.System.out; // NOSONAR
import static java.util.function.Predicate.not;
import static org.nqm.utils.GisStringUtils.isNotBlank;
import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.coloringWord;
import static org.nqm.utils.StdOutUtils.errln;
import static org.nqm.utils.StdOutUtils.infof;
import static org.nqm.utils.StdOutUtils.warnln;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.utils.GisStringUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class CommandVerticle extends AbstractVerticle {

  private final String[] commandWithArgs;
  private final Path path;

  public CommandVerticle(Path path, String... args) {
    this.path = path;
    this.commandWithArgs = new String[args.length + 1];
    this.commandWithArgs[0] = GisConfig.GIT_HOME_DIR;
    for (int i = 0; i < args.length; i++) {
      this.commandWithArgs[i + 1] = args[i];
    }
    GisLog.debug("executing command '%s' under directory '%s'", commandWithArgs, path);
    GisVertx.eventAddDir(path);
  }

  @Override
  public void start() {
    vertx.executeBlocking(
      (Promise<Process> promise) -> {
        try {
          promise.complete(new ProcessBuilder(commandWithArgs).directory(path.toFile()).start());
        }
        catch (IOException e) {
          errln(e.getMessage());
          GisLog.debug(e);
        }
      },
      false,
      res -> {
        Optional.of(res.result()).ifPresent(this::safelyPrint);
        GisVertx.eventRemoveDir(path);
      });
  }

  private void safelyPrint(Process pr) {
    var line = "";
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var sb = new StringBuilder(infof("%s", "" + path.getFileName()));
    try {
      while (isNotBlank(line = input.readLine())) {
        if (commandWithArgs[1].equals("status")) {
          sb.append(gitStatus(line));
        }
        else {
          sb.append("\n  ").append(line);
        }
      }
      out.println(sb.toString());
      Optional.of(pr.waitFor())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug("exit with code: '%s'".formatted(exitCode));
          warnln("Could not perform on module: '%s'".formatted(this.path.getFileName()));
        });
    }
    catch (IOException e) {
      errln(e.getMessage());
      GisLog.debug(e);
    }
    catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
    }
  }

  private static String gitStatus(String line) {
    var sb = new StringBuilder();
    if (line.startsWith("# branch.oid")) {
      return "";
    }
    if (line.startsWith("# branch.head")) {
      sb.append("\n  ## ").append(coloringWord(line.split("\s")[2], CL_GREEN));
    }
    else if (line.startsWith("# branch.upstream")) {
      sb.append("...").append(coloringWord(line.split("\s")[2], CL_RED));
    }
    else if (line.startsWith("# branch.ab")) {
      Optional.of(line.split("\s"))
        .map(CommandVerticle::buildAheadBehind)
        .filter(GisStringUtils::isNotBlank)
        .map(" [%s]"::formatted)
        .ifPresent(sb::append);
    }
    else {
      final var immutableLine = line;
      UnaryOperator<String> getFiles = filesChange -> immutableLine.startsWith("2")
        ? Optional.of(filesChange.split("\t")).map(s -> s[1] + " -> " + s[0]).orElse("")
        : filesChange;

      Optional.of(line.split("\s"))
        .ifPresent(splitS -> sb.append("\n  ")
          .append(Optional.of(splitS[1].toCharArray()).map(CommandVerticle::buildStaging).orElse(""))
          .append(Optional.of(splitS[splitS.length - 1]).map(getFiles).orElse("")));
    }
    return sb.toString();
  }

  private static String buildAheadBehind(String[] splitS) {
    var ahead = Optional.of(splitS[2])
      .map(s -> s.replace("+", ""))
      .filter(not("0"::equals))
      .map(s -> "ahead " + coloringWord(s, CL_GREEN))
      .orElse("");
    var behind = Optional.of(splitS[3])
      .map(s -> s.replace("-", ""))
      .filter(not("0"::equals))
      .map(s -> "behind " + coloringWord(s, CL_RED))
      .orElse("");
    return Stream.of(ahead, behind).filter(not(String::isBlank)).collect(Collectors.joining(", "));
  }

  private static String buildStaging(char[] chars) {
    return Optional.of(chars[0])
      .map(s -> s != '.' ? coloringWord(s, CL_GREEN) : s + "")
      .orElse("") +
      Optional.of(chars[1])
        .map(s -> s != '.' ? coloringWord(s, CL_RED) : s + "")
        .orElse("")
      + " ";
  }
}
