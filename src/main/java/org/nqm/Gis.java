package org.nqm;

import org.nqm.command.GitCommand;
import org.nqm.config.GisLog;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
  name = "gis",
  description = "Git extension wrapper which supports submodules",
  mixinStandardHelpOptions = true,
  version = "1.0.1")
public class Gis extends GitCommand {

  @Option(names = "-v", description = "Show more details information.", scope = ScopeType.INHERIT)
  public void setVerbose(boolean verbose) {
    GisLog.setIsDebugEnabled(verbose);
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      new CommandLine(new Gis()).execute("status");
      return;
    }
    new CommandLine(new Gis()).execute(args);
  }

}
