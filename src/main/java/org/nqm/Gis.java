package org.nqm;

import org.nqm.command.GisCommand;
import org.nqm.command.GisVersion;
import org.nqm.config.GisLog;
import org.nqm.utils.GisProcessUtils;
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
    versionProvider = GisVersion.class)
public class Gis extends GisCommand {

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

  @Option(names = "--dry-run", description = "Show command output, do not execute.", scope = ScopeType.INHERIT)
  public static void setDryRun(boolean dryRun) {
    GisProcessUtils.isDryRunEnabled(dryRun);
  }

  public static void main(String... args) {
    // try {
    //   System.out.println(GitStatusCommand.status(Path.of("/home/minh/projects/test/small-git-root-module/small-git-domain/.git"), false));
    // } catch (NoWorkTreeException | IOException | GitAPIException e) {
    //   e.printStackTrace();
    // }

    var gis = new CommandLine(new Gis());
    gis.setExecutionExceptionHandler(GLOBAL_EXCEPTION_HANLER);
    gis.execute(args.length == 0
        ? new String[] {GIT_STATUS, "--one-line"}
        : args);
  }
}
