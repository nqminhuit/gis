package org.nqm;

import static java.lang.System.err;
import static java.lang.System.out;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

final class GitWrapper {

    private static final String GIT = "/usr/bin/git %s";

    private GitWrapper() {}

    public static void status() {
        run(path -> call(path, "status -sb --ignore-submodules"), err::println);
    }

    public static void fetch() {
        run(path -> call(path, "fetch"), err::println);
    }

    public static void pull() {
        run(path -> call(path, "pull"), err::println);
    }

    public static void checkOut(String branch) {
        run(path -> call(path, "checkout %s".formatted(branch)),
            () -> err.println("Could not checkout branch '%s'".formatted(branch)));
    }

    public static void checkOutNewBranch(String branch) {
        run(path -> call(path, "checkout -b %s".formatted(branch)), err::println);
    }


    private static void run(Function<Path, Integer> consume, Runnable errHandling) {
        var gitModulesFilePath = Path.of(".", ".gitmodules");
        if (!gitModulesFilePath.toFile().exists()) {
            out.println("There is no git submodules under this directory!");
            return;
        }

        var currentDir = System.getProperty("user.dir");

        Consumer<? super Integer> handleError = exitCode -> {
            if (exitCode != 0) {
                errHandling.run();
            }
        };

        Optional.of(consume.apply(Path.of(currentDir))).ifPresent(handleError);

        getSubModuleDirectories(gitModulesFilePath)
            .forEach(dir -> Optional.of(consume.apply(Path.of(currentDir, dir))).ifPresent(handleError));
    }

    private static int call(Path path, String command) {
        if (!path.toFile().exists()) {
            // err.println("path '%s' is not found".formatted(path.toString())); TODO make this debugable
            return 1;
        }

        out.println("Entering '%s'".formatted(path.toString()));
        try {
            var pr = Runtime.getRuntime().exec(GIT.formatted(command), null, path.toFile());
            var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            var line = "";
            while ((line = input.readLine()) != null) {
                out.println(line);
            }
            return pr.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<String> getSubModuleDirectories(Path path) {
        try {
            return Files.readAllLines(path).stream()
                .map(String::trim)
                .filter(s -> s.startsWith("path"))
                .map(s -> s.replace("path = ", ""));
        }
        catch (IOException e) {
            return Stream.of();
        }
    }

}
