package org.nqm;

import static java.lang.System.out;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

final class GitWrapper {

    private GitWrapper() {}

    public static void status() {
        var gitModulesFilePath = Path.of(".", ".gitmodules");
        if (!gitModulesFilePath.toFile().exists()) {
            out.println("There is no git submodules under this directory!");
            return;
        }

        var currentDir = System.getProperty("user.dir");
        status(Path.of(currentDir));
        getSubModuleDirectories(gitModulesFilePath).forEach(dir -> status(Path.of(currentDir, dir)));
    }

    private static void status(Path path) {
        if (!path.toFile().exists()) {
            return;
        }

        try {
            out.println("Entering '%s'".formatted(path.toString()));
            var pr = Runtime.getRuntime()
                .exec("/usr/bin/git status -sb --ignore-submodules", null, path.toFile());
            var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            var line = "";
            while ((line = input.readLine()) != null) {
                out.println(line);
            }
            pr.waitFor();
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
