package org.nqm.utils;

import static java.lang.System.err; // NOSONAR
import static java.lang.System.out; // NOSONAR
import static java.util.function.Predicate.not;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StdOutUtils {

  private StdOutUtils() {}

  public static final String CL_RESET = "\u001B[0m";
  public static final String CL_BLACK = "\u001B[30m";
  public static final String CL_RED = "\u001B[31m";
  public static final String CL_GREEN = "\u001B[32m";
  public static final String CL_YELLOW = "\u001B[33m";
  public static final String CL_BLUE = "\u001B[34m";
  public static final String CL_PURPLE = "\u001B[35m";
  public static final String CL_CYAN = "\u001B[36m";
  public static final String CL_WHITE = "\u001B[37m";

  public static void errln(String msg) {
    err.println("  " + CL_RED + "ERROR: " + msg + CL_RESET);
  }

  public static void warnln(String msg) {
    err.println("  " + CL_YELLOW + "WARNING: " + msg + CL_RESET);
  }

  public static void debugln(String msg) {
    out.println("  " + CL_YELLOW + "[DEBUG] " + msg + CL_RESET);
  }

  public static String infof(String msgFormat, String word) {
    return msgFormat.formatted(CL_CYAN + word + CL_RESET);
  }

  public static String coloringWord(String word, String color) {
    return color + word + CL_RESET;
  }

  public static String coloringWord(Character c, String color) {
    return color + c + CL_RESET;
  }

  public static String buildStaging(char[] chars) {
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
    var sb = new StringBuilder();
    if (line.startsWith("# branch.oid")) {
      return "";
    }
    if (line.startsWith("# branch.head")) {
      sb.append("\n  ## ").append(coloringWord(line.split("\s")[2], CL_GREEN));
    }
    else if (line.startsWith("# branch.upstream")) {
      sb.append("...").append(coloringWord(line.split("\s")[2], CL_RED));
    }
    else if (line.startsWith("# branch.ab")) {
      Optional.of(line.split("\s"))
        .map(StdOutUtils::buildAheadBehind)
        .filter(GisStringUtils::isNotBlank)
        .map(" [%s]"::formatted)
        .ifPresent(sb::append);
    }
    else {
      final var immutableLine = line;
      UnaryOperator<String> getFiles = filesChange -> immutableLine.startsWith("2")
        ? Optional.of(filesChange.split("\t")).map(s -> s[1] + " -> " + s[0]).orElse("")
        : filesChange;

      Optional.of(line.split("\s"))
        .ifPresent(splitS -> sb.append("\n  ")
          .append(Optional.of(splitS[1].toCharArray()).map(StdOutUtils::buildStaging).orElse(""))
          .append(Optional.of(splitS[splitS.length - 1]).map(getFiles).orElse("")));
    }
    return sb.toString();
  }
}
