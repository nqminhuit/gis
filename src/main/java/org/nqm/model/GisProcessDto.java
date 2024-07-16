package org.nqm.model;

import static org.nqm.utils.GisStringUtils.NEWLINE;
import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.RG_AHEAD_NUM;
import static org.nqm.utils.StdOutUtils.RG_BEHIND_NUM;
import static org.nqm.utils.StdOutUtils.RG_STAGED_STATUS;
import static org.nqm.utils.StdOutUtils.RG_UNSTAGED_STATUS;
import static org.nqm.utils.StdOutUtils.RG_LOCAL_BRANCH;
import static org.nqm.utils.StdOutUtils.RG_UP_STREAM_BRANCH;
import static org.nqm.utils.StdOutUtils.coloringBranch;
import static org.nqm.utils.StdOutUtils.coloringFirstRegex;
import static org.nqm.utils.StdOutUtils.coloringWord;
import static org.nqm.utils.StdOutUtils.extractFirstRegexMatched;
import static org.nqm.utils.StdOutUtils.infof;
import static org.nqm.utils.StdOutUtils.warnln;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.nqm.config.GisLog;
import org.nqm.utils.GisStringUtils;

public record GisProcessDto(String output, int exitCode, String directory) {

  private static final String WARN_MSG_FMT = "Could not perform on module: '%s'";
  private static final String EXIT_WITH_CODE_MSG_FMT = "exit with code: '%s'";

  private void verifyExitCode() {
    Optional.of(exitCode)
        .filter(exitCode -> exitCode != 0)
        .ifPresent(exitCode -> {
          GisLog.debug(EXIT_WITH_CODE_MSG_FMT.formatted(exitCode));
          warnln(WARN_MSG_FMT.formatted(directory));
        });
  }

  public String parseGitStatus(boolean oneLiner) {
    verifyExitCode();
    var sb = new StringBuilder(infof(directory));
    if (oneLiner) {
      return sb.append(Stream.of(output.split(NEWLINE))
          .map(GisProcessDto::buildStatusOneLiner)
          .collect(Collectors.joining(" ")))
          .toString();
    }
    return sb.append(NEWLINE)
        .append(Stream.of(output.split(NEWLINE))
            .map(GisProcessDto::buildStatusFull)
            .map("  %s"::formatted)
            .collect(Collectors.joining(NEWLINE)))
        .toString();
  }

  private static String buildStatusFull(String s) {
    var colored = s;
    if (s.startsWith("##")) {
      colored = coloringFirstRegex(colored, RG_LOCAL_BRANCH, CL_GREEN);
      colored = coloringFirstRegex(colored, RG_UP_STREAM_BRANCH, CL_RED);
      colored = coloringFirstRegex(colored, RG_AHEAD_NUM, CL_GREEN);
      colored = coloringFirstRegex(colored, RG_BEHIND_NUM, CL_RED);
    } else {
      colored = coloringFirstRegex(colored, RG_UNSTAGED_STATUS, CL_RED);
      colored = coloringFirstRegex(colored, RG_STAGED_STATUS, CL_GREEN);
    }
    return colored;
  }

  private static String buildStatusOneLiner(String s) {
    if (s.startsWith("##")) {
      var coloredBranch = coloringBranch(extractFirstRegexMatched(s, RG_LOCAL_BRANCH));
      var ahead = Optional.of(extractFirstRegexMatched(s, RG_AHEAD_NUM))
          .filter(Predicate.not(String::isBlank))
          .map(n -> coloringWord(n, CL_GREEN))
          .map("ahead "::concat)
          .orElse("");
      var behind = Optional.of(extractFirstRegexMatched(s, RG_BEHIND_NUM))
          .filter(Predicate.not(String::isBlank))
          .map(n -> coloringWord(n, CL_RED))
          .map("behind "::concat)
          .orElse("");
      var coloredAheadBehind = Stream.of(ahead, behind)
          .filter(Predicate.not(String::isBlank))
          .collect(Collectors.joining(", "));
      if (GisStringUtils.isBlank(coloredAheadBehind)) {
        return " " + coloredBranch;
      }
      return " %s [%s]".formatted(coloredBranch, coloredAheadBehind);
    }
    if (s.startsWith("R  ")) {
      return "'%s'".formatted(s.substring("R  ".length()));
    }
    return Stream.of(s.split("\s")).reduce((a, b) -> b).orElse("");
  }
}
