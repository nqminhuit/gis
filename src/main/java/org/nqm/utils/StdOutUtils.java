package org.nqm.utils;

import static java.lang.System.err; // NOSONAR
import static java.lang.System.out; // NOSONAR
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.nqm.config.GisConfig;

public class StdOutUtils {

  private StdOutUtils() {}

  private static boolean muteOutput = false;

  public static final String CL_RESET = "\u001B[0m";
  public static final String CL_BLACK = "\u001B[30m";
  public static final String CL_RED = "\u001B[31m";
  public static final String CL_GREEN = "\u001B[32m";
  public static final String CL_YELLOW = "\u001B[33m";
  public static final String CL_BLUE = "\u001B[34m";
  public static final String CL_PURPLE = "\u001B[35m";
  public static final String CL_CYAN = "\u001B[36m";
  public static final String CL_WHITE = "\u001B[37m";

  public static final String RG_LOCAL_BRANCH = "(\\w+\\/\\w+)|(\\w+)";
  public static final String RG_UP_STREAM_BRANCH = "(origin\\/\\w+\\/\\w+)|(origin\\/\\w+)";
  public static final String RG_AHEAD_NUM = "(?<=ahead\s)\\d+";
  public static final String RG_BEHIND_NUM = "(?<=behind\s)\\d+";
  public static final String RG_STAGED_STATUS = "(^[ADRUM?])";
  public static final String RG_UNSTAGED_STATUS = "(?<=([ADRUM?])|(^\s))[\\w?]";

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

  public static String coloringBranch(String branch) {
    if (Stream.of(GisConfig.getDefaultBranches()).anyMatch(branch::equals)) {
      return coloringWord(branch, CL_RED);
    }

    if (Stream.of(GisConfig.getFeatureBranchPrefixes()).anyMatch(branch::startsWith)) {
      return coloringWord(branch, CL_YELLOW);
    }

    return coloringWord(branch, CL_GREEN);
  }

  public static String extractFirstRegexMatched(String wholeText, String regex) {
    var matcher = Pattern.compile(regex).matcher(wholeText);
    var matched = "";
    if (matcher.find()) {
      matched = matcher.group();
    }
    return matched;
  }

  public static String coloringFirstRegex(String wholeText, String regex, String color) {
    var matcher = Pattern.compile(regex).matcher(wholeText);
    var word = "";
    if (matcher.find()) {
      word = matcher.group();
    }
    return matcher.replaceFirst(color + word + CL_RESET);
  }

  public static String coloringWord(String word, String color) {
    return color + word + CL_RESET;
  }

}
