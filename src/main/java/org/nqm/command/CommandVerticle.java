package org.nqm.command;

import static org.nqm.utils.StdOutUtils.gitStatus;
import static org.nqm.utils.StdOutUtils.gitStatusOneLine;
import static org.nqm.utils.StdOutUtils.infof;
import static org.nqm.utils.StdOutUtils.warnln;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
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

  private static String[] prependCommandToArgs(String... args) {
    return Stream.concat(
        Stream.of(GisConfig.GIT_HOME_DIR),
        Stream.of(args).takeWhile(arg -> !arg.startsWith(OPTION_PREFIX)))
        .toArray(String[]::new);
  }

  public static GisProcessDto executeForDto(Path path, String... args) {
    if (path == null) {
      return GisProcessDto.EMPTY;
    }
    var commandWithArgs = prependCommandToArgs(args);

    GisLog.debug("executing command '%s' under module '%s'", commandWithArgs, path);

    try {
      return GisProcessUtils.run(path.toFile(), commandWithArgs);
    } catch (IOException e) {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    } catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
      throw new GisException(e.getMessage());
    }
  }

  public static String execute(Path path, String... args) {
    if (path == null) {
      throw new GisException("path must not be null");
    }
    var result = executeForDto(path, args);
    var gisOptions = Stream.of(args)
        .filter(arg -> arg.startsWith(OPTION_PREFIX))
        .toArray(String[]::new);

    if (args[0].equals(GitCommand.GIT_STATUS)) {
      return safelyPrintStatus(path, args, result);
    }
    if (Stream.of(gisOptions).anyMatch(GIS_NO_PRINT_MODULES_NAME_OPT::equals)) {
      return safelyPrintWithoutModules(path, result);
    }
    if (Stream.of(gisOptions).anyMatch(GIS_CONCAT_MODULES_NAME_OPT::equals)) {
      return safelyConcatModuleNames(path, result);
    }
    return safelyPrint(path, result);
  }

  private static String safelyPrintStatus(Path path, String[] gisOptions, GisProcessDto result) {
    var sb = new StringBuilder(infof("" + path.getFileName()));
    var isOneLineOpt = Stream.of(gisOptions).anyMatch("--gis-one-line"::equals);
    Stream.of(result.output().split(GisStringUtils.NEWLINE))
        .filter(GisStringUtils::isNotBlank)
        .forEach(line -> sb.append(isOneLineOpt ? gitStatusOneLine(line) : gitStatus(line)));
    Optional.of(result.exitCode())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(path.getFileName()));
        });
    return sb.toString();
  }

  private static String safelyPrint(Path path, GisProcessDto result) {
    var sb = new StringBuilder(infof("" + path.getFileName()));
    Stream.of(result.output().split(GisStringUtils.NEWLINE))
        .filter(GisStringUtils::isNotBlank)
        .forEach(line -> sb.append("%n  %s".formatted(line)));
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

  private static String safelyConcatModuleNames(Path path, GisProcessDto result) {
    if (GisStringUtils.isBlank(result.output())) {
      return "";
    }
    var sb = new StringBuilder();
    var isRootModule = path.toFile().listFiles((d, f) -> {
      var stringF = "" + f;
      return ".gitmodules".equals(stringF) || ".gis-modules".equals(stringF);
    }).length > 0;

    var shortPath = path.getFileName();
    Stream.of(result.output().split(GisStringUtils.NEWLINE))
        .filter(GisStringUtils::isNotBlank)
        .forEach(line -> {
          var f = "%s/%s".formatted(path, line);
          if (!new File(f).isFile()) {
            return;
          }
          if (isRootModule) {
            sb.append("./%s%n".formatted(line));
          } else {
            sb.append("%s/%s%n".formatted(shortPath, line));
          }
        });
    Optional.of(result.exitCode())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(path.getFileName()));
        });
    return sb.toString().trim();
  }
}
