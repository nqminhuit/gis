package org.nqm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.nqm.model.GisBranchStatus;
import org.nqm.model.GisFileChange;
import org.nqm.model.GisModuleStatus;

/**
 * Parses the output of `git status -sb --porcelain=v1` into the model classes. Both the
 * human readable rendering in {@link StdOutUtils} and the structured output share this parsing.
 */
public class GitStatusParser {

  private GitStatusParser() {}

  private static final String BRANCH_PREFIX = "## ";
  private static final String DETACHED_HEAD = "HEAD (no branch)";
  private static final String DETACHED_BRANCH = "HEAD(detached)";
  private static final String NO_COMMITS_YET = "No commits yet on ";
  private static final String INITIAL_COMMIT = "Initial commit on ";
  private static final String UPSTREAM_SEPARATOR = "\\.\\.\\.";
  private static final String RENAME_SEPARATOR = " -> ";
  private static final String AHEAD = "ahead ";
  private static final String BEHIND = "behind ";
  private static final String GONE = "gone";

  public static boolean isBranchLine(String line) {
    return line.startsWith(BRANCH_PREFIX);
  }

  /**
   * Splits a branch line into its branch part (which may still carry the upstream) and the
   * tracking part which git puts between square brackets.
   */
  public static String[] splitBranchLine(String line) {
    var branchDetails = line.substring(BRANCH_PREFIX.length());
    // detached HEAD (e.g. a submodule checked out at a SHA); keep the branch a single
    // token so the positional parsing of --sort still works
    if (branchDetails.equals(DETACHED_HEAD)) {
      return new String[] {DETACHED_BRANCH, ""};
    }
    if (branchDetails.startsWith(NO_COMMITS_YET)) {
      return new String[] {branchDetails.substring(NO_COMMITS_YET.length()), ""};
    }
    if (branchDetails.startsWith(INITIAL_COMMIT)) {
      return new String[] {branchDetails.substring(INITIAL_COMMIT.length()), ""};
    }
    var split = branchDetails.split(" \\[", 2);
    return new String[] {split[0], split.length > 1 ? split[1].replaceFirst("]$", "") : ""};
  }

  public static char[] extractStatus(String line) {
    return line.substring(0, Math.min(2, line.length())).toCharArray();
  }

  private static String normalizePathToken(String path) {
    var normalized = path.trim();
    if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
      normalized = normalized.substring(1, normalized.length() - 1)
          .replace("\\\"", "\"")
          .replace("\\\\", "\\");
    }
    return normalized;
  }

  // ' -> ' separates the two paths only on rename/copy lines; anywhere else it is
  // just part of a file name
  private static boolean isRenameOrCopyStatus(String line) {
    if (line.length() < 2) {
      return false;
    }
    var x = line.charAt(0);
    var y = line.charAt(1);
    return x == 'R' || x == 'C' || y == 'R' || y == 'C';
  }

  /**
   * The paths of a status line: a single element, or the source and the destination of a
   * rename/copy.
   */
  public static String[] extractPaths(String line) {
    if (line.length() <= 3) {
      return new String[] {""};
    }
    var paths = line.substring(3);
    if (!isRenameOrCopyStatus(line)) {
      return new String[] {normalizePathToken(paths)};
    }
    return Stream.of(paths.split(RENAME_SEPARATOR))
        .map(GitStatusParser::normalizePathToken)
        .toArray(String[]::new);
  }

  public static GisBranchStatus parseBranchLine(String line) {
    var detached = line.substring(BRANCH_PREFIX.length()).equals(DETACHED_HEAD);
    var branchSplit = splitBranchLine(line);
    var upstreamSplit = branchSplit[0].split(UPSTREAM_SEPARATOR, 2);
    var tracking = branchSplit[1];
    return new GisBranchStatus(
        detached ? "HEAD" : upstreamSplit[0],
        upstreamSplit.length > 1 ? upstreamSplit[1] : null,
        trackingCount(tracking, AHEAD),
        trackingCount(tracking, BEHIND),
        Stream.of(tracking.split(", ")).map(String::trim).anyMatch(GONE::equals),
        detached);
  }

  private static int trackingCount(String tracking, String prefix) {
    return Stream.of(tracking.split(", "))
        .map(String::trim)
        .filter(s -> s.startsWith(prefix))
        .map(s -> s.substring(prefix.length()).trim())
        .mapToInt(GitStatusParser::toIntOrZero)
        .findFirst()
        .orElse(0);
  }

  private static int toIntOrZero(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static GisFileChange parseFileLine(String line) {
    var status = extractStatus(line);
    var paths = extractPaths(line);
    return new GisFileChange(
        statusAt(status, 0),
        statusAt(status, 1),
        paths[paths.length - 1],
        paths.length > 1 ? paths[0] : null);
  }

  private static String statusAt(char[] status, int idx) {
    return status.length > idx ? "" + status[idx] : "";
  }

  public static GisModuleStatus parse(String module, boolean isRootModule, String output) {
    GisBranchStatus branch = null;
    var files = new ArrayList<GisFileChange>();
    for (var line : output.split(GisStringUtils.NEWLINE)) {
      if (GisStringUtils.isBlank(line)) {
        continue;
      }
      if (isBranchLine(line)) {
        branch = parseBranchLine(line);
      } else {
        files.add(parseFileLine(line));
      }
    }
    return new GisModuleStatus(module, isRootModule, branch, List.copyOf(files));
  }
}
