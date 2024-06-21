package org.nqm.vertx;

import static org.nqm.command.GitCommand.HOOKS_OPTION;
import static org.nqm.utils.GisStringUtils.isNotBlank;
import static org.nqm.utils.StdOutUtils.errln;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.command.GitCommand;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import org.nqm.utils.GisStringUtils;
import org.nqm.utils.StdOutUtils;
import io.vertx.core.Future;

public class CommandVerticle {

  private static final String WARN_MSG_FMT = "Could not perform on module: '%s'";
  private static final String EXIT_WITH_CODE_MSG_FMT = "exit with code: '%s'";
  private static final String OPTION_PREFIX = "--gis";
  private final String[] commandWithArgs;
  private String[] gisOptions;
  private final String commandHook;
  private final Path path;

  public CommandVerticle(Path path, String... args) {
    this.path = path;
    this.commandHook = extractHookCommand(args);
    this.commandWithArgs = buildCommandWithArgs(args);
    GisLog.debug("executing command '%s' under module '%s'", commandWithArgs, path.getFileName());
    GisVertx.eventAddDir(path);
  }

  public CommandVerticle() {
    this.path = null;
    this.commandWithArgs = null;
    this.commandHook = null;
    GisVertx.eventAddDir(Path.of("."));
  }

  /**
   * Hooks should be placed at the end of command for optimize
   */
  private String extractHookCommand(String... args) {
    var predicate = Predicate.not(HOOKS_OPTION::equals);
    return Stream.of(args)
        .dropWhile(predicate)
        .filter(predicate)
        .collect(Collectors.joining(" "));
  }

  /**
   * For optimal hooks and options should be placed at the end of command
   */
  private String[] buildCommandWithArgs(String... args) {
    this.gisOptions = Stream.of(args)
      .filter(arg -> arg.startsWith(OPTION_PREFIX))
      .toArray(String[]::new);

    return Stream.concat(
      Stream.of(GisConfig.GIT_HOME_DIR),
      Stream.of(args).takeWhile(arg -> !arg.startsWith(OPTION_PREFIX) && !HOOKS_OPTION.equals(arg)))
      .toArray(String[]::new);
  }

  public void execute() {
    if (path == null) {
      return;
    }
    Process res;
    try {
      res = new ProcessBuilder(commandWithArgs).directory(path.toFile()).start();
    } catch (IOException e) {
      GisLog.debug(e);
      return;
    }

    if (GisStringUtils.isNotBlank(this.commandHook)) {
      gisExecuteCommand(res, this.commandHook)
          .forEach(f -> f.onComplete(r -> Optional.ofNullable(r.result()).ifPresent(p -> {
            try {
              infof("%s", new String(p.getInputStream().readAllBytes()));
            } catch (IOException e) {
              errln(e.getMessage());
              GisLog.debug(e);
            }
          })));
    } else if (Stream.of(gisOptions).anyMatch("--gis-no-print-modules-name"::equals)) {
      safelyPrintWithoutModules(res);
    } else if (Stream.of(gisOptions).anyMatch("--gis-concat-modules-name"::equals)) {
      safelyConcatModuleNames(res);
    } else {
      safelyPrint(res);
    }
  }

  // public void start() {
  //   if (path == null) {
  //     GisVertx.eventRemoveDir(Path.of("."));
  //     return;
  //   }
  //   vertx.executeBlocking(() -> new ProcessBuilder(commandWithArgs).directory(path.toFile()).start(), false)
  //       .compose(res -> {
  //         if (GisStringUtils.isNotBlank(this.commandHook)) {
  //           gisExecuteCommand(res, this.commandHook)
  //               .forEach(f -> f.onComplete(r -> Optional.ofNullable(r.result()).ifPresent(p -> {
  //                 try {
  //                   infof("%s", new String(p.getInputStream().readAllBytes()));
  //                 } catch (IOException e) {
  //                   errln(e.getMessage());
  //                   GisLog.debug(e);
  //                 }
  //               })));
  //         } else if (Stream.of(gisOptions).anyMatch("--gis-no-print-modules-name"::equals)) {
  //           safelyPrintWithoutModules(res);
  //         } else if (Stream.of(gisOptions).anyMatch("--gis-concat-modules-name"::equals)) {
  //           safelyConcatModuleNames(res);
  //         } else {
  //           safelyPrint(res);
  //         }
  //         return Future.succeededFuture();
  //       })
  //       .onComplete(r -> GisVertx.eventRemoveDir(path));
  // }

  private void safelyPrint(Process pr) {
    var line = "";
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var sb = new StringBuilder(infof("%s", "" + path.getFileName()));
    var isOneLineOpt = Stream.of(gisOptions).anyMatch("--gis-one-line"::equals);
    var isStatusCmd = commandWithArgs[1].equals(GitCommand.GIT_STATUS);
    try {
      while (isNotBlank(line = input.readLine())) {
        if (isStatusCmd) {
          sb.append(isOneLineOpt ? gitStatusOneLine(line) : gitStatus(line));
        }
        else {
          sb.append("%n  %s".formatted(line));
        }
      }
      StdOutUtils.println(sb.toString());
      Optional.of(pr.waitFor())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(this.path.getFileName()));
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

  private void safelyPrintWithoutModules(Process pr) {
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var sb = new StringBuilder();
    try {
      var line = "";
      while (isNotBlank(line = input.readLine())) {
        sb.append("%s%n".formatted(line));
      }
      StdOutUtils.print(sb.toString());
      Optional.of(pr.waitFor())
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(this.path.getFileName()));
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

  private List<Future<Process>> gisExecuteCommand(Process pr, String cmd) {
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var futures = new ArrayList<Future<Process>>();
    try {
      var line = "";
      while (isNotBlank(line = input.readLine())) {
        futures.add(Future.succeededFuture(
          new ProcessBuilder(cmd.formatted(line).split(" "))
            .directory(path.toFile())
            .start()));
      }
    }
    catch (IOException e) {
      errln(e.getMessage());
      GisLog.debug(e);
    }
    return futures;
  }

  private void safelyConcatModuleNames(Process pr) {
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var sb = new StringBuilder();
    var isRootModule = path.toFile()
        .listFiles((d, f) -> {
          var stringF = "" + f;
          return ".gitmodules".equals(stringF) || ".gis-modules".equals(stringF);
        }).length > 0;

    var line = "";
    var shortPath = path.getFileName();

    try {
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
      StdOutUtils.print(sb.toString());
      Optional.of(pr.waitFor())
          .filter(exitCode -> exitCode != 0)
          .ifPresent(exitCode -> {
            GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
            warnln(WARN_MSG_FMT.formatted(this.path.getFileName()));
          });
    } catch (IOException e) {
      errln(e.getMessage());
      GisLog.debug(e);
    } catch (InterruptedException e) {
      GisLog.debug(e);
      Thread.currentThread().interrupt();
    }
  }
}
