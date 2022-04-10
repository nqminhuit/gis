package org.nqm.vertx;

import static java.lang.System.out; // NOSONAR
import static org.nqm.utils.GisStringUtils.isNotBlank;
import static org.nqm.utils.StdOutUtils.errln;
import static org.nqm.utils.StdOutUtils.gitStatus;
import static org.nqm.utils.StdOutUtils.infof;
import static org.nqm.utils.StdOutUtils.warnln;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import org.nqm.config.GisConfig;
import org.nqm.config.GisLog;
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
    GisLog.debug("executing command '%s' under module '%s'", commandWithArgs, path.getFileName());
    GisVertx.eventAddDir(path);
  }

  public CommandVerticle() {
    this.path = null;
    this.commandWithArgs = null;
    GisVertx.eventAddDir(Path.of("."));
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
        sb.append(commandWithArgs[1].equals("status") ? gitStatus(line) : "%n  %s".formatted(line));
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
}
