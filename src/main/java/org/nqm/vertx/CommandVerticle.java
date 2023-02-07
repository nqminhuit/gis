package org.nqm.vertx;

import static java.lang.System.out; // NOSONAR
import static org.nqm.utils.GisStringUtils.isNotBlank;
import static org.nqm.utils.StdOutUtils.errln;
import static org.nqm.utils.StdOutUtils.gitStatus;
import static org.nqm.utils.StdOutUtils.gitStatusOneLine;
import static org.nqm.utils.StdOutUtils.infof;
import static org.nqm.utils.StdOutUtils.warnln;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.nqm.command.GitCommand;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class CommandVerticle extends AbstractVerticle {

  private final String[] commandWithArgs;
  private String gisOption;
  private final Path path;

  public CommandVerticle(Path path, String... args) {
    this.path = path;
    this.commandWithArgs = buildCommandWithArgs(args);
    GisLog.debug("executing command '%s' under module '%s'", commandWithArgs, path.getFileName());
    GisVertx.eventAddDir(path);
  }

  public CommandVerticle() {
    this.path = null;
    this.commandWithArgs = null;
    GisVertx.eventAddDir(Path.of("."));
  }

  private String[] buildCommandWithArgs(String... args) {
    var cmdWithArgs = new String[args.length + 1];
    cmdWithArgs[0] = GisConfig.GIT_HOME_DIR;
    var n = args.length;
    for (var i = 0; i < n - 1; i++) {
      cmdWithArgs[i + 1] = args[i];
    }
    // for better performance it is to required all '--gis' options to be at the end of cmd
    var lastArg = args[n - 1];
    if (args[n - 1].startsWith("--gis")) {
      this.gisOption = lastArg;
    }
    else {
      cmdWithArgs[n] = lastArg;
    }
    return Stream.of(cmdWithArgs).filter(Objects::nonNull).toArray(String[]::new);
  }

  @Override
  public void start() {
    if (path == null) {
      GisVertx.eventRemoveDir(Path.of("."));
      return;
    }
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
      false)
      .compose(res -> {
        if ("--gis-execute".equals(gisOption)) {
          executeCommand(res).forEach(f -> {
            f.onComplete(r -> {
              Optional.ofNullable(r.result()).ifPresent(p -> {
                try {
                  infof("%s", new String(p.getInputStream().readAllBytes()));
                }
                catch (IOException e) {
                  errln(e.getMessage());
                  GisLog.debug(e);
                }
              });
            });
          });
        }
        else if ("--gis-no-print-modules-name".equals(gisOption)) {
          safelyPrintWithoutModules(res);
        }
        else {
          safelyPrint(res);
        }
        return Future.succeededFuture();
      })
      .onComplete(r -> GisVertx.eventRemoveDir(path));
  }

  private void safelyPrint(Process pr) {
    var line = "";
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var sb = new StringBuilder(infof("%s", "" + path.getFileName()));
    var isOneLineOpt = "--gis-one-line".equals(gisOption);
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

  private void safelyPrintWithoutModules(Process pr) {
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var sb = new StringBuilder();
    try {
      var line = "";
      while (isNotBlank(line = input.readLine())) {
        sb.append("%s%n".formatted(line));
      }
      out.print(sb.toString());
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

  private List<Future<Process>> executeCommand(Process pr) {
    var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
    var futures = new ArrayList<Future<Process>>();
    try {
      var line = "";
      while (isNotBlank(line = input.readLine())) {
        futures
          .add(Future.succeededFuture(new ProcessBuilder(GisConfig.GIT_HOME_DIR, "branch", "-d", line)
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

}
