package org.nqm;

import static java.lang.System.err;
import static org.nqm.GitWrapper.*;
import org.nqm.enums.GisOption;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "gis",
    description = "Git extension which supports submodules",
    mixinStandardHelpOptions = true,
    version = "1.0.0")
public class GisCommand implements Runnable {

    @Parameters(index = "0", description = "Valid values: ${COMPLETION-CANDIDATES}")
    GisOption option;

    @Parameters(index = "1", arity = "0..1")
    String value;

    public static void main(String[] args) throws Exception {
        System.exit(new CommandLine(new GisCommand()).execute(args));
    }

    @Override
    public void run() {
        switch (option) {
            case co:
                Optional.ofNullable(value)
                    .ifPresentOrElse(
                        GitWrapper::checkOut,
                        () -> err.println("Please specified branch name!"));
                break;
            case st:
                status();
                break;
            case fe:
                fetch();
                break;
            case pu:
                pull();
                break;
            default:
                status();
        }
    }

}
