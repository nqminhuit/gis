package org.nqm.utils;

import static java.lang.System.err; // NOSONAR
import static java.lang.System.out; // NOSONAR

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

  public static final String FONT_BOLD = "\u001B[1m";

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
}
