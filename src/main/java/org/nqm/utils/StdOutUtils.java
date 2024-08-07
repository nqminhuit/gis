package org.nqm.utils;

import static java.lang.System.err; // NOSONAR
import static java.lang.System.out; // NOSONAR
import static java.util.function.Predicate.not;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.config.GisConfig;

public class StdOutUtils {

  private StdOutUtils() {}

  private static boolean muteOutput = false;

  public static final String CL_RESET  = "\u001B[0m";
  public static final String CL_BLACK  = "\u001B[30m";
  public static final String CL_RED    = "\u001B[31m";
  public static final String CL_GREEN  = "\u001B[32m";
  public static final String CL_YELLOW = "\u001B[33m";
  public static final String CL_BLUE   = "\u001B[34m";
  public static final String CL_PURPLE = "\u001B[35m";
  public static final String CL_CYAN   = "\u001B[36m";
  public static final String CL_WHITE  = "\u001B[37m";

  private static final String UNTRACKED_SYM = "?";
  private static final String RENAME_SYM = "2";

  public static void setMuteOutput(boolean b) {
    muteOutput = b;
  }

  public static void println(String msg) {
    if (muteOutput) {
      return;
    }
    out.println(msg);
  }

  public static void print(String msg) {
    if (muteOutput) {
      return;
    }
    out.print(msg);
  }

  public static void errln(String msg) {
    err.println("  " + CL_RED + "ERROR: " + msg + CL_RESET);
  }

  public static void warnln(String msg) {
    err.println("  " + CL_YELLOW + "WARNING: " + msg + CL_RESET);
  }

  public static void debugln(String msg) {
    out.println("  " + CL_YELLOW + "[DEBUG] " + msg + CL_RESET);
  }

  public static String infof(String word) {
    return "%s".formatted(CL_CYAN + word + CL_RESET);
  }

  private static String coloringBranch(String branch) {
    if (Stream.of(GisConfig.getDefaultBranches()).anyMatch(branch::equals)) {
      return coloringWord(branch, CL_RED);
    }

    if (Stream.of(GisConfig.getFeatureBranchPrefixes()).anyMatch(branch::startsWith)) {
      return coloringWord(branch, CL_YELLOW);
    }

    return coloringWord(branch, CL_GREEN);
  }

  private static String coloringWord(String word, String color) {
    return color + word + CL_RESET;
  }

  private static String coloringWord(Character c, String color) {
    return color + c + CL_RESET;
  }

  private static String buildStaging(char[] chars) {
    return Optional.of(chars[0])
      .map(s -> s != '.' ? coloringWord(s, CL_GREEN) : s + "")
      .orElse("") +
      Optional.of(chars[1])
        .map(s -> s != '.' ? coloringWord(s, CL_RED) : s + "")
        .orElse("")
      + " ";
  }

  private static String buildAheadBehind(String[] splitS) {
    var ahead = Optional.of(splitS[2])
      .map(s -> s.replace("+", ""))
      .filter(not("0"::equals))
      .map(s -> "ahead " + coloringWord(s, CL_GREEN))
      .orElse("");
    var behind = Optional.of(splitS[3])
      .map(s -> s.replace("-", ""))
      .filter(not("0"::equals))
      .map(s -> "behind " + coloringWord(s, CL_RED))
      .orElse("");
    return Stream.of(ahead, behind).filter(not(String::isBlank)).collect(Collectors.joining(", "));
  }

  public static String gitStatus(String line) {
    var lineSplit = line.split("\s");
    return switch (lineSplit[0] + lineSplit[1]) {
      case "#branch.oid" -> "";
      case "#branch.head" -> "\n  ## " + coloringWord(lineSplit[2], CL_GREEN);
      case "#branch.upstream" -> "..." + coloringWord(lineSplit[2], CL_RED);
      case "#branch.ab" -> Optional.of(lineSplit)
        .map(StdOutUtils::buildAheadBehind)
        .filter(GisStringUtils::isNotBlank)
        .map(" [%s]"::formatted)
        .orElse("");
      default -> Optional.of(lineSplit)
        .map(StdOutUtils::preProcessUntrackFile)
        .map(splitS -> "\n  "
          + Optional.of(splitS[1].toCharArray()).map(StdOutUtils::buildStaging).orElse("")
          + Optional.of(splitS[splitS.length - 1]).map(getFiles(line)).orElse(""))
        .orElse("");
    };
  }

  public static String gitStatusOneLine(String line) {
    var lineSplit = line.split("\s");
    return switch (lineSplit[0] + lineSplit[1]) {
      case "#branch.oid" -> "";
      case "#branch.head" -> " " + coloringBranch(lineSplit[2]);
      case "#branch.upstream" -> "";
      case "#branch.ab" -> Optional.of(lineSplit)
        .map(StdOutUtils::buildAheadBehind)
        .filter(GisStringUtils::isNotBlank)
        .map("[%s]"::formatted)
        .orElse("");
      default -> Optional.of(lineSplit)
        .map(splitS -> " "
          + Optional.of(splitS[splitS.length - 1])
            .map(getFiles(line))
            .map(l -> Path.of(l).getFileName().toString())
            .orElse(""))
        .orElse("");
    };
  }

  private static String[] preProcessUntrackFile(String[] fileStats) {
    var length = fileStats.length;
    if (!UNTRACKED_SYM.equals(fileStats[0])) {
      return fileStats;
    }
    var newStats = new String[length + 1];
    newStats[0] = fileStats[0];
    newStats[1] = " " + UNTRACKED_SYM;
    for (var i = 1; i < length; i++) {
      newStats[i + 1] = fileStats[i];
    }
    return newStats;
  }

  private static UnaryOperator<String> getFiles(String line) {
    return filesChange -> line.startsWith(RENAME_SYM)
      ? Optional.of(filesChange.split("\t")).map(s -> s[1] + " -> " + s[0]).orElse("")
      : filesChange;
  }
}
