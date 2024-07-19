package org.nqm.command;

import static org.nqm.command.GitCommand.HOOKS_OPTION;
import static org.nqm.utils.GisStringUtils.isNotBlank;
import static org.nqm.utils.StdOutUtils.gitStatus;
import static org.nqm.utils.StdOutUtils.gitStatusOneLine;
import static org.nqm.utils.StdOutUtils.infof;
import static org.nqm.utils.StdOutUtils.warnln;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.GisException;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.model.GisProcessDto;
import org.nqm.utils.GisProcessUtils;
import org.nqm.utils.GisStringUtils;

public class CommandVerticle {

  private CommandVerticle() {}

  private static final String WARN_MSG_FMT = "Could not perform on module: '%s'";
  private static final String EXIT_WITH_CODE_MSG_FMT = "exit with code: '%s'";
  private static final String OPTION_PREFIX = "--gis";

  public static final String GIS_NO_PRINT_MODULES_NAME_OPT = "--gis-no-print-modules-name";
  public static final String GIS_CONCAT_MODULES_NAME_OPT = "--gis-concat-modules-name";

  /**
   * Hooks should be placed at the end of command for optimize
   */
  private static String extractHookCommand(String... args) {
    var predicate = Predicate.not(HOOKS_OPTION::equals);
    return Stream.of(args)
        .dropWhile(predicate)
        .filter(predicate)
        .collect(Collectors.joining(" "));
  }

  /**
   * For optimal hooks and options should be placed at the end of command
   */
  private static String[] buildCommandWithArgs(String... args) {
    return Stream.concat(
        Stream.of(GisConfig.GIT_HOME_DIR),
        Stream.of(args).takeWhile(arg -> !arg.startsWith(OPTION_PREFIX) && !HOOKS_OPTION.equals(arg)))
        .toArray(String[]::new);
  }

  public static String execute(Path path, String... args) {
    if (path == null) {
      return "";
    }
    var commandHook = extractHookCommand(args);
    var commandWithArgs = buildCommandWithArgs(args);
    var gisOptions = Stream.of(args)
        .filter(arg -> arg.startsWith(OPTION_PREFIX))
        .toArray(String[]::new);

    GisLog.debug("executing command '%s' under module '%s'", commandWithArgs, path);

    try {
      var result = GisProcessUtils.run(path.toFile(), commandWithArgs);
      if (GisStringUtils.isNotBlank(commandHook)) {
        var output = new ConcurrentLinkedQueue<String>();
        gisExecuteCommand(path, GisStringUtils.toInputStreamReader(result.output()), commandHook)
            .forEach(p -> output.add(infof("%s", p.output())));
        return String.join(GisStringUtils.NEWLINE, output);
      } else if (Stream.of(gisOptions).anyMatch(GIS_NO_PRINT_MODULES_NAME_OPT::equals)) {
        return safelyPrintWithoutModules(path, result);
      } else if (Stream.of(gisOptions).anyMatch(GIS_CONCAT_MODULES_NAME_OPT::equals)) {
        return safelyConcatModuleNames(path, result);
      } else {
        return safelyPrint(path, gisOptions, commandWithArgs, result);
      }
    } catch (IOException e) {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    } catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
  }

  private static String safelyPrint(
      Path path, String[] gisOptions, String[] commandWithArgs, GisProcessDto result) throws IOException {
    var line = "";
    var input = new BufferedReader(GisStringUtils.toInputStreamReader(result.output()));
    var sb = new StringBuilder(infof("%s", "" + path.getFileName()));
    var isOneLineOpt = Stream.of(gisOptions).anyMatch("--gis-one-line"::equals);
    while (isNotBlank(line = input.readLine())) {
      if (commandWithArgs[1].equals(GitCommand.GIT_STATUS)) {
        sb.append(isOneLineOpt ? gitStatusOneLine(line) : gitStatus(line));
      } else {
        sb.append("%n  %s".formatted(line));
      }
    }
    Optional.of(result.exitCode())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(path.getFileName()));
        });
    return sb.toString();
  }

  private static String safelyPrintWithoutModules(Path path, GisProcessDto result) {
    Optional.of(result.exitCode())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(path.getFileName()));
        });
    return result.output().trim();
  }

  private static List<GisProcessDto> gisExecuteCommand(Path path, InputStreamReader stream, String cmd)
      throws IOException, InterruptedException {
    var input = new BufferedReader(stream);
    var futures = new ArrayList<GisProcessDto>();
    var line = "";
    while (isNotBlank(line = input.readLine())) {
      futures.add(GisProcessUtils.run(path.toFile(), cmd.formatted(line).split(" ")));
    }
    return futures;
  }

  private static String safelyConcatModuleNames(Path path, GisProcessDto result) throws IOException {
    if (GisStringUtils.isBlank(result.output())) {
      return "";
    }
    var input = new BufferedReader(GisStringUtils.toInputStreamReader(result.output()));
    var sb = new StringBuilder();
    var isRootModule = path.toFile()
        .listFiles((d, f) -> {
          var stringF = "" + f;
          return ".gitmodules".equals(stringF) || ".gis-modules".equals(stringF);
        }).length > 0;

    var line = "";
    var shortPath = path.getFileName();

    while (isNotBlank(line = input.readLine())) {
      var f = "%s/%s".formatted(path, line);
      if (!new File(f).isFile()) {
        continue;
      }
      if (isRootModule) {
        sb.append("./%s%n".formatted(line));
      } else {
        sb.append("%s/%s%n".formatted(shortPath, line));
      }
    }
    Optional.of(result.exitCode())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(path.getFileName()));
        });
    return sb.toString().trim();
  }
}
