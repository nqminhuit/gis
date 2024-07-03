package org.nqm;

import org.nqm.command.GitCommand;
import org.nqm.config.GisLog;
import org.nqm.exception.GisException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;

@Command(
    name = "gis",
    description = "Git extension wrapper which supports submodules",
    mixinStandardHelpOptions = true,
    version = "2.0.0-dev")
public class Gis extends GitCommand {

  private static final IExecutionExceptionHandler GLOBAL_EXCEPTION_HANLER = new IExecutionExceptionHandler() {
    @Override
    public int handleExecutionException(Exception e, CommandLine commandLine, ParseResult fullParseResult)
        throws Exception {
      GisLog.debug(e);
      throw new GisException(e.getMessage());
    }
  };

  @Option(names = "-v", description = "Show more details information.", scope = ScopeType.INHERIT)
  public static void setVerbose(boolean verbose) {
    GisLog.setIsDebugEnabled(verbose);
  }

  public static void main(String... args) {
    var gis = new CommandLine(new Gis());
    gis.setExecutionExceptionHandler(GLOBAL_EXCEPTION_HANLER);

    gis.execute(args.length == 0
        ? new String[] {GIT_STATUS, "--one-line"}
        : args);
  }
}
