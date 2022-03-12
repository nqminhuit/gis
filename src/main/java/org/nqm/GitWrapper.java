package org.nqm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

final class GitWrapper {

    private GitWrapper() {}

    public static void status() {
        try {
            var pr = Runtime.getRuntime().exec("/usr/bin/git status -sb --ignore-submodules");
            var input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            var line = "";
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            pr.waitFor();
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
