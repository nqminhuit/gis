package org.nqm;

import io.micronaut.configuration.picocli.PicocliRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "gis",
    description = "Git extension which supports submodules",
    mixinStandardHelpOptions = true,
    version = "1.0.0")
public class GisCommand implements Runnable {

    @Option(names = { "st", "status" })
    boolean status;

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(GisCommand.class, args);
    }

    @Override
    public void run() {
        if (status) {
            GitWrapper.status();
        }
    }
}
