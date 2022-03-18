package org.nqm;

import static org.nqm.GitWrapper.fetch;
import static org.nqm.GitWrapper.pull;
import static org.nqm.GitWrapper.status;
import static org.nqm.utils.StdOutUtils.errln;
import org.nqm.enums.GisAction;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
  name = "gis",
  description = "Git extension which supports submodules",
  mixinStandardHelpOptions = true,
  version = "1.0.0-alpha")
public class GisCommand implements Runnable {

  // TODO: replace action with picocli sub commands
  @Parameters(index = "0", description = "Valid values: ${COMPLETION-CANDIDATES}")
  GisAction action;

  @Parameters(index = "1", arity = "0..1")
  Optional<String> value;

  @Option(names = "-v", description = "verbose")
  boolean isDebugEnabled;

  public static void main(String[] args) {
    var cmd = new CommandLine(new GisCommand());
    // cmd.setCaseInsensitiveEnumValuesAllowed(true);
    cmd.execute(args);
  }

  @Override
  public void run() {
    switch (action) {
      case co -> value.ifPresentOrElse(GitWrapper::checkOut, () -> errln("Please specify branch!"));
      case fe -> fetch();
      case pu -> pull();
      case st -> status();
      default -> status();
    }
  }

}
