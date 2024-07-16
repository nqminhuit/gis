package org.nqm.model;

import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.coloringWord;
import java.util.List;

public enum FileStatus {

  // @formatter:off
  MODIFIED(1, "M"),
  CHANGED(0, "M"),
  ADDED(0, "A"),
  MISSING(1, "D"),
  REMOVED(0, "D"),
  UNTRACKED(1, "?"),
  CONFLICT(1, "U"),
  UNTRACKED_DIRS(1, "?");
  // @formatter:on

  /**
   * The level base on staging, 0 means staged, 1 means unstagged
   */
  private int level;

  private String symbol;

  FileStatus(int level, String symbol) {
    this.level = level;
    this.symbol = symbol;
  }

  public static String toPrint(String file, List<FileStatus> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return "";
    }
    var size = statuses.size();
    var result = "%s%s %s";
    if (size > 1) {
      statuses.sort((a, b) -> a.level - b.level);
      var staged = statuses.getFirst();
      var unstaged = statuses.getLast();
      result = result.formatted(
          coloringWord(staged.symbol, CL_GREEN),
          coloringWord(unstaged.symbol, CL_RED),
          file);
    } else {
      var st = statuses.getFirst();
      int level = st.level;
      if (level == 0) {
        result = result.formatted(coloringWord(st.symbol, CL_GREEN), ".", file);
      } else {
        result = result.formatted(".", coloringWord(st.symbol, CL_RED), file);
      }
    }
    return result;
  }
}
