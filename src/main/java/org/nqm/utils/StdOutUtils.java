package org.nqm.utils;

import static java.lang.System.err; // NOSONAR
import static java.lang.System.out; // NOSONAR
import static java.util.function.Predicate.not;
import java.nio.file.Path;
import java.util.Optional;
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
  public static final String CL_GRAY   = "\u001B[90m";

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

  private static boolean isRootDontCareFile(String file) {
    var path = Path.of(file);
    if (path.getNameCount() != 1) {
      return false;
    }

    var fileName = path.getFileName().toString();
    return Stream.of(Optional.ofNullable(GisConfig.getDontCareFiles()).orElseGet(() -> new String[] {}))
        .map(String::trim)
        .filter(GisStringUtils::isNotBlank)
        .anyMatch(fileName::equals);
  }

  private static String coloringFile(String file, boolean isRootModule) {
    return coloringFile(file, file, isRootModule);
  }

  private static String coloringFile(String pathToMatch, String fileToDisplay, boolean isRootModule) {
    if (!isRootModule) {
      return fileToDisplay;
    }

    return Stream.of(pathToMatch.split(" -> "))
        .allMatch(StdOutUtils::isRootDontCareFile)
        ? coloringWord(fileToDisplay, CL_GRAY)
        : fileToDisplay;
  }

  private static String buildStaging(char[] chars) {
    if (chars.length == 2 && chars[0] == '?' && chars[1] == '?') {
      return " ? ";
    }
    return Optional.of(chars[0])
      .map(StdOutUtils::normalizeUnchanged)
      .map(s -> s != '.' ? coloringWord(s, CL_GREEN) : s + "")
      .orElse("") +
      Optional.of(chars[1])
        .map(StdOutUtils::normalizeUnchanged)
        .map(s -> s != '.' ? coloringWord(s, CL_RED) : s + "")
        .orElse("")
      + " ";
  }

  private static char normalizeUnchanged(char status) {
    return status == ' ' ? '.' : status;
  }

  private static boolean isBranchLine(String line) {
    return line.startsWith("## ");
  }

  private static String[] splitBranchLine(String line) {
    var branchDetails = line.substring(3);
    if (branchDetails.startsWith("No commits yet on ")) {
      return new String[] {branchDetails.substring("No commits yet on ".length()), ""};
    }
    if (branchDetails.startsWith("Initial commit on ")) {
      return new String[] {branchDetails.substring("Initial commit on ".length()), ""};
    }
    var split = branchDetails.split(" \\[", 2);
    return new String[] {split[0], split.length > 1 ? split[1].replaceFirst("]$", "") : ""};
  }

  private static String buildBranchInfo(String branchLine) {
    var branchSplit = splitBranchLine(branchLine);
    var upstreamSplit = branchSplit[0].split("\\.\\.\\.", 2);
    var branch = "\n  ## " + coloringWord(upstreamSplit[0], CL_GREEN);
    if (upstreamSplit.length == 1) {
      return branch;
    }
    var upstream = "..." + coloringWord(upstreamSplit[1], CL_RED);
    if (GisStringUtils.isBlank(branchSplit[1])) {
      return branch + upstream;
    }
    var tracking = Optional.of(branchSplit[1].split(", "))
        .map(StdOutUtils::buildAheadBehindV1)
        .filter(GisStringUtils::isNotBlank)
        .map(" [%s]"::formatted)
        .orElse("");
    return branch + upstream + tracking;
  }

  private static String buildBranchInfoOneLine(String branchLine) {
    var branchSplit = splitBranchLine(branchLine);
    var branch = " " + coloringBranch(branchSplit[0].split("\\.\\.\\.", 2)[0]);
    if (GisStringUtils.isBlank(branchSplit[1])) {
      return branch;
    }
    return branch + Optional.of(branchSplit[1].split(", "))
        .map(StdOutUtils::buildAheadBehindV1)
        .filter(GisStringUtils::isNotBlank)
        .map("[%s]"::formatted)
        .orElse("");
  }

  private static String buildAheadBehindV1(String[] splitS) {
    return Stream.of(splitS)
        .map(String::trim)
        .map(s -> {
          if (s.startsWith("ahead ")) {
            return "ahead " + coloringWord(s.substring("ahead ".length()), CL_GREEN);
          }
          if (s.startsWith("behind ")) {
            return "behind " + coloringWord(s.substring("behind ".length()), CL_RED);
          }
          return "";
        })
        .filter(not(String::isBlank))
        .collect(Collectors.joining(", "));
  }

  private static char[] extractStatus(String line) {
    return line.substring(0, Math.min(2, line.length())).toCharArray();
  }

  private static String extractFile(String line) {
    return line.length() <= 3 ? "" : line.substring(3);
  }

  public static String gitStatus(String line) {
    return gitStatus(line, false);
  }

  public static String gitStatus(String line, boolean isRootModule) {
    if (isBranchLine(line)) {
      return buildBranchInfo(line);
    }
    return "\n  "
        + buildStaging(extractStatus(line))
        + coloringFile(extractFile(line), isRootModule);
  }

  public static String gitStatusOneLine(String line) {
    return gitStatusOneLine(line, false);
  }

  public static String gitStatusOneLine(String line, boolean isRootModule) {
    if (isBranchLine(line)) {
      return buildBranchInfoOneLine(line);
    }
    var file = extractFile(line);
    return " " + coloringFile(file, Path.of(file).getFileName().toString(), isRootModule);
  }
}
