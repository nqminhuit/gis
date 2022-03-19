package org.nqm;

import org.nqm.command.GitCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  name = "gis",
  description = "Git extension wrapper which supports submodules",
  mixinStandardHelpOptions = true,
  version = "1.0.0-alpha")
public class Gis extends GitCommand {

  @Option(names = "-v", description = "verbose")
  boolean isDebugEnabled;

  public static void main(String[] args) {
    new CommandLine(new Gis()).execute(args);
  }

}
