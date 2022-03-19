package org.nqm.vertx;

import static java.lang.System.out;
import static org.nqm.utils.ExceptionUtils.throwIf;
import static org.nqm.utils.GisStringUtils.isNotBlank;
import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_PURPLE;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.FONT_BOLD;
import static org.nqm.utils.StdOutUtils.coloringWord;
import static org.nqm.utils.StdOutUtils.infof;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class CommandVerticle extends AbstractVerticle {

  private static final String SPACES_NOT_INSIDE_SQUARE_BRACKETS = "\\s((?<!\\[.*)|(?!.*\\]))";

  private final String[] commandWithArgs;
  private final Path path;
  private final boolean colorOutput;

  public CommandVerticle(Path path, boolean colorOutput, String... args) {
    this.path = path;
    this.colorOutput = colorOutput;

    this.commandWithArgs = new String[args.length + 1];
    this.commandWithArgs[0] = "/usr/bin/git";
    for (int i = 0; i < args.length; i++) {
      this.commandWithArgs[i + 1] = args[i];
    }
  }

  @Override
  public void start() {
    vertx.executeBlocking(
      (Promise<Process> promise) -> {
        try {
          promise.complete(new ProcessBuilder(commandWithArgs).directory(path.toFile()).start());
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
    var sb = new StringBuilder(infof("Entering '%s'", "" + path.getFileName())).append('\n');
    try {
      while (isNotBlank(line = input.readLine())) {
        sb.append(colorOutput ? coloringOuput(line) : line).append('\n');
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

  private static String coloringOuput(String line) {
    var words = Stream.of(line.split(SPACES_NOT_INSIDE_SQUARE_BRACKETS))
      .filter(Predicate.not(String::isBlank))
      .toArray(String[]::new);

    try {
      String startWord = words[0];
      if (!startWord.startsWith("##")) {
        words[0] = coloringWord(startWord, CL_RED);
      }
      else {
        words[1] = coloringWord(FONT_BOLD + words[1], CL_PURPLE);
      }

      words[2] = coloringWord(words[2], CL_GREEN);
    }
    catch (ArrayIndexOutOfBoundsException e) {
      // just ignore it
    }
    return Stream.of(words).collect(Collectors.joining(" "));
  }

}
