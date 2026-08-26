package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.Test;

class GitStatusParserTest {

  private static String output(String... lines) {
    return String.join(GisStringUtils.NEWLINE, lines);
  }

  @Test
  void parse_withBranchAndFiles_OK() {
    // when:
    var module = GitStatusParser.parse("gis", true, output(
        "## master...origin/master [ahead 2, behind 9]",
        " M pom.xml",
        "M  README.md",
        "?? target/",
        "R  text-1 -> text-0001"));

    // then:
    assertThat(module.module()).isEqualTo("gis");
    assertThat(module.root()).isTrue();
    assertThat(module.branch().name()).isEqualTo("master");
    assertThat(module.branch().upstream()).isEqualTo("origin/master");
    assertThat(module.branch().ahead()).isEqualTo(2);
    assertThat(module.branch().behind()).isEqualTo(9);
    assertThat(module.branch().gone()).isFalse();
    assertThat(module.branch().detached()).isFalse();
    assertThat(module.files()).extracting("indexStatus", "worktreeStatus", "path", "originalPath")
        .containsExactly(
            tuple(" ", "M", "pom.xml", null),
            tuple("M", " ", "README.md", null),
            tuple("?", "?", "target/", null),
            tuple("R", " ", "text-0001", "text-1"));
  }

  @Test
  void parse_withoutUpstream_OK() {
    // when:
    var branch = GitStatusParser.parse("m", false, "## feature/abc").branch();

    // then:
    assertThat(branch.name()).isEqualTo("feature/abc");
    assertThat(branch.upstream()).isNull();
    assertThat(branch.ahead()).isZero();
    assertThat(branch.behind()).isZero();
  }

  @Test
  void parse_withAheadOnly_OK() {
    // when:
    var branch = GitStatusParser.parse("m", false, "## master...origin/master [ahead 10]").branch();

    // then:
    assertThat(branch.ahead()).isEqualTo(10);
    assertThat(branch.behind()).isZero();
  }

  @Test
  void parse_withBehindOnly_OK() {
    // when:
    var branch = GitStatusParser.parse("m", false, "## master...origin/master [behind 20]").branch();

    // then:
    assertThat(branch.ahead()).isZero();
    assertThat(branch.behind()).isEqualTo(20);
  }

  @Test
  void parse_withGoneUpstream_OK() {
    // when:
    var branch = GitStatusParser.parse("m", false, "## master...origin/master [gone]").branch();

    // then:
    assertThat(branch.upstream()).isEqualTo("origin/master");
    assertThat(branch.gone()).isTrue();
  }

  @Test
  void parse_withDetachedHead_OK() {
    // when:
    var branch = GitStatusParser.parse("m", false, "## HEAD (no branch)").branch();

    // then:
    assertThat(branch.name()).isEqualTo("HEAD");
    assertThat(branch.upstream()).isNull();
    assertThat(branch.detached()).isTrue();
  }

  @Test
  void parse_withoutCommitYet_OK() {
    // then:
    assertThat(GitStatusParser.parse("m", false, "## No commits yet on master").branch().name())
        .isEqualTo("master");
    assertThat(GitStatusParser.parse("m", false, "## Initial commit on master").branch().name())
        .isEqualTo("master");
  }

  @Test
  void parse_withArrowInFileName_shouldNotSplitPath() {
    // when:
    var files = GitStatusParser.parse("m", false, output("?? \"weird -> name.txt\"", " M a -> b.txt")).files();

    // then:
    assertThat(files).extracting("path").containsExactly("weird -> name.txt", "a -> b.txt");
    assertThat(files).extracting("originalPath").containsOnlyNulls();
  }

  @Test
  void parse_withoutAnyOutput_OK() {
    // when:
    var module = GitStatusParser.parse("m", false, "");

    // then:
    assertThat(module.branch()).isNull();
    assertThat(module.files()).isEmpty();
  }

  @Test
  void parse_withNotANumberTracking_shouldFallBackToZero() {
    // when:
    var branch = GitStatusParser.parse("m", false, "## master...origin/master [ahead x]").branch();

    // then:
    assertThat(branch.ahead()).isZero();
  }
}
