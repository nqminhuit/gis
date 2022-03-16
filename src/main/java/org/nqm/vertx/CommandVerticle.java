package org.nqm.vertx;

import static java.lang.System.out;
import static org.nqm.utils.ExceptionUtils.throwIf;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class CommandVerticle extends AbstractVerticle {

    private final String git;
    private final String command;
    private final Path path;

    public CommandVerticle(String git, String command, Path path) {
        this.git = git;
        this.command = command;
        this.path = path;
    }

    @Override
    public void start() {
        vertx.executeBlocking(
            (Promise<Process> promise) -> {
                try {
                    promise.complete(Runtime.getRuntime().exec(git.formatted(command), null, path.toFile()));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            false,
            res -> {
                Optional.of(res.result()).ifPresent(this::safelyPrint);
                // root should be considered the last dir
                if (System.getProperty("user.dir").equals(path.toString())) {
                    System.exit(0);
                }
            });
    }

    private void safelyPrint(Process pr) {
        var line = "";
        var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        var sb = new StringBuilder("Entering '%s'".formatted(path.toString())).append('\n');
        try {
            while (isNotBlank(line = input.readLine())) {
                sb.append(line).append('\n');
            }
            out.print(sb.toString());
            Optional.of(pr.waitFor())
                .ifPresent(exitCode -> throwIf(exitCode != 0,
                    () -> new RuntimeException("Process exits with code: '%s'".formatted(exitCode))));
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
