package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nqm.utils.StdOutUtils.CL_CYAN;
import static org.nqm.utils.StdOutUtils.CL_GRAY;
import static org.nqm.utils.StdOutUtils.CL_GREEN;
import static org.nqm.utils.StdOutUtils.CL_RED;
import static org.nqm.utils.StdOutUtils.CL_RESET;
import static org.nqm.utils.StdOutUtils.CL_YELLOW;
import org.junit.jupiter.api.Test;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.StdBaseTest;

class StdOutUtilsTest extends StdBaseTest {

  @Override
  protected void additionalSetup() {
    GisConfigMock.mockBranchesColorDefault();
  }

  @Override
  protected void additionalTeardown() {
    GisConfigMock.close();
  }

  @Test
  void debugln_OK() {
    StdOutUtils.debugln("debugln_7_b");
    assertThat(outCaptor.toByteArray())
        .isEqualTo(("  " + CL_YELLOW + "[DEBUG] debugln_7_b" + CL_RESET + "\n").getBytes())
        .containsExactly(
            32, 32, 27, 91, 51, 51, 109, 91, 68, 69, 66, 85, 71, 93, 32, 100,
            101, 98, 117, 103, 108, 110, 95, 55, 95, 98, 27, 91, 48, 109, 10);
  }

  @Test
  void errln_OK() {
    StdOutUtils.errln("errLn_q_1");
    assertThat(errCaptor.toByteArray())
        .isEqualTo(("  " + CL_RED + "ERROR: errLn_q_1" + CL_RESET + "\n").getBytes())
        .containsExactly(
            32, 32, 27, 91, 51, 49, 109, 69, 82, 82, 79, 82, 58, 32, 101,
            114, 114, 76, 110, 95, 113, 95, 49, 27, 91, 48, 109, 10);
  }

  @Test
  void warnln_OK() {
    StdOutUtils.warnln("warnln_8+h");
    assertThat(errCaptor.toByteArray())
        .isEqualTo(("  " + CL_YELLOW + "WARNING: warnln_8+h" + CL_RESET + "\n").getBytes())
        .containsExactly(
            32, 32, 27, 91, 51, 51, 109, 87, 65, 82, 78, 73, 78, 71, 58, 32,
            119, 97, 114, 110, 108, 110, 95, 56, 43, 104, 27, 91, 48, 109, 10);
  }

  @Test
  void infof_OK() {
    assertThat(StdOutUtils.infof("batmen").getBytes())
        .isEqualTo((CL_CYAN + "batmen" + CL_RESET).getBytes());
  }

  private static String coloringWord(String word, String color) {
    return color + word + CL_RESET;
  }

  @Test
  void gitStatus_OK() {
    assertThat(StdOutUtils.gitStatus("## master", false))
        .isEqualTo("\n  ## %s".formatted(coloringWord("master", CL_GREEN)));
    assertThat(StdOutUtils.gitStatus("## master...origin/master", false))
        .isEqualTo("\n  ## %s...%s".formatted(coloringWord("master", CL_GREEN), coloringWord("origin/master", CL_RED)));
    assertThat(StdOutUtils.gitStatus("## master...origin/master [ahead 2, behind 9]", false))
        .isEqualTo("\n  ## %s...%s [ahead %s, behind %s]"
            .formatted(
                coloringWord("master", CL_GREEN),
                coloringWord("origin/master", CL_RED),
                coloringWord("2", CL_GREEN),
                coloringWord("9", CL_RED)));
    assertThat(StdOutUtils.gitStatus("## master...origin/master [ahead 10]", false))
        .isEqualTo("\n  ## %s...%s [ahead %s]"
            .formatted(coloringWord("master", CL_GREEN), coloringWord("origin/master", CL_RED), coloringWord("10", CL_GREEN)));
    assertThat(StdOutUtils.gitStatus("## master...origin/master [behind 20]", false))
        .isEqualTo("\n  ## %s...%s [behind %s]"
            .formatted(coloringWord("master", CL_GREEN), coloringWord("origin/master", CL_RED), coloringWord("20", CL_RED)));
    assertThat(StdOutUtils.gitStatus(" M pom.xml", false))
            .isEqualTo("\n  .%s pom.xml".formatted(coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatus("M  pom.xml", false))
            .isEqualTo("\n  %s. pom.xml".formatted(coloringWord("M", CL_GREEN)));
    assertThat(StdOutUtils.gitStatus("MM pom.xml", false))
            .isEqualTo("\n  %s%s pom.xml".formatted(coloringWord("M", CL_GREEN), coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatus("AM src/test/java/org/nqm/utils/StdOutUtilsTest.java", false))
            .isEqualTo("\n  %s%s src/test/java/org/nqm/utils/StdOutUtilsTest.java"
                .formatted(coloringWord("A", CL_GREEN), coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatus("R  text-1 -> text-0001", false))
            .isEqualTo("\n  %s. text-1 -> text-0001".formatted(coloringWord("R", CL_GREEN)));
  }

  @Test
  void gitStatusOneLine_OK() {
    assertThat(StdOutUtils.gitStatusOneLine("## master", false))
        .isEqualTo(" %s".formatted(coloringWord("master", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## main", false))
        .isEqualTo(" %s".formatted(coloringWord("main", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## develop", false))
        .isEqualTo(" %s".formatted(coloringWord("develop", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## feature/destroy-d-b", false))
        .isEqualTo(" %s".formatted(coloringWord("feature/destroy-d-b", CL_YELLOW)));
    assertThat(StdOutUtils.gitStatusOneLine("## NO-TICKETeee", false))
        .isEqualTo(" %s".formatted(coloringWord("NO-TICKETeee", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine("## master...origin/master", false))
        .isEqualTo(" %s".formatted(coloringWord("master", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## master...origin/master [ahead 2, behind 9]", false))
        .isEqualTo(
            " %s[ahead %s, behind %s]".formatted(coloringWord("master", CL_RED), coloringWord("2", CL_GREEN), coloringWord("9", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## master...origin/master [ahead 10]", false))
        .isEqualTo(" %s[ahead %s]".formatted(coloringWord("master", CL_RED), coloringWord("10", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine("## master...origin/master [behind 20]", false))
        .isEqualTo(" %s[behind %s]".formatted(coloringWord("master", CL_RED), coloringWord("20", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine(" M pom.xml", false))
            .isEqualTo(" pom.xml");
    assertThat(StdOutUtils.gitStatusOneLine("M  pom.xml", false))
            .isEqualTo(" pom.xml");
    assertThat(StdOutUtils.gitStatusOneLine("MM pom.xml", false))
            .isEqualTo(" pom.xml");
    assertThat(StdOutUtils.gitStatusOneLine("AM src/test/java/org/nqm/utils/StdOutUtilsTest.java", false))
            .isEqualTo(" StdOutUtilsTest.java");
  }

  @Test
  void gitStatusOneLine_withConfigBranchPrefixes_shouldCompareCaseSensitive() {
    // given:
    GisConfigMock.mockBranchesColorDefault(new String[]{"Master", "MAIN"}, new String[]{"FEA-ture"});

    // then:
    assertThat(StdOutUtils.gitStatusOneLine("## Master", false))
        .isEqualTo(" %s".formatted(coloringWord("Master", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## masTer", false))
        .isEqualTo(" %s".formatted(coloringWord("masTer", CL_GREEN)));

    assertThat(StdOutUtils.gitStatusOneLine("## MAIN", false))
        .isEqualTo(" %s".formatted(coloringWord("MAIN", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("## main", false))
        .isEqualTo(" %s".formatted(coloringWord("main", CL_GREEN)));

    assertThat(StdOutUtils.gitStatusOneLine("## FEA-ture/destroy-d-b", false))
        .isEqualTo(" %s".formatted(coloringWord("FEA-ture/destroy-d-b", CL_YELLOW)));
    assertThat(StdOutUtils.gitStatusOneLine("## Fea-ture/destroy-d-b", false))
        .isEqualTo(" %s".formatted(coloringWord("Fea-ture/destroy-d-b", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine("## fea-ture/destroy-d-b", false))
        .isEqualTo(" %s".formatted(coloringWord("fea-ture/destroy-d-b", CL_GREEN)));
  }

  @Test
  void gitStatus_withDontCareFilesUnderRoot_shouldUseGrayColor() {
    // given:
    GisConfigMock.mockDontCareFiles("pom.xml", ".editorconfig");

    // then:
    assertThat(StdOutUtils.gitStatus(
        " M pom.xml",
        true))
            .isEqualTo("\n  .%s %s".formatted(coloringWord("M", CL_RED), coloringWord("pom.xml", CL_GRAY)));
    assertThat(StdOutUtils.gitStatusOneLine(
        " M pom.xml",
        true))
            .isEqualTo(" %s".formatted(coloringWord("pom.xml", CL_GRAY)));
  }

  @Test
  void gitStatus_withDontCareFilesOutsideRoot_shouldNotUseGrayColor() {
    // given:
    GisConfigMock.mockDontCareFiles("pom.xml");

    // then:
    assertThat(StdOutUtils.gitStatusOneLine(
        " M pom.xml",
        false))
            .isEqualTo(" pom.xml");
  }

  @Test
  void gitStatus_withNestedRootFile_shouldNotMatchRootDontCareFile() {
    // given:
    GisConfigMock.mockDontCareFiles("pom.xml");

    // then:
    assertThat(StdOutUtils.gitStatus(
        " M config/pom.xml",
        true))
            .isEqualTo("\n  .%s config/pom.xml".formatted(coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine(
        " M config/pom.xml",
        true))
            .isEqualTo(" pom.xml");
  }

  @Test
  void gitStatus_withRenameTouchingNestedFile_shouldNotMatchRootDontCareFile() {
    // given:
    GisConfigMock.mockDontCareFiles("pom.xml");

    // then:
    assertThat(StdOutUtils.gitStatus(
        "R  pom.xml -> service-a/pom.xml",
        true))
            .isEqualTo("\n  %s. pom.xml -> service-a/pom.xml".formatted(coloringWord("R", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine(
        "R  pom.xml -> service-a/pom.xml",
        true))
            .isEqualTo(" pom.xml");
  }

  @Test
  void gitStatus_withRootRenameBetweenDontCareFiles_shouldUseGrayColor() {
    // given:
    GisConfigMock.mockDontCareFiles("pom.xml", "launch.json");

    // then:
    assertThat(StdOutUtils.gitStatus(
        "R  pom.xml -> launch.json",
        true))
            .isEqualTo("\n  %s. %s"
                .formatted(coloringWord("R", CL_GREEN), coloringWord("pom.xml -> launch.json", CL_GRAY)));
    assertThat(StdOutUtils.gitStatusOneLine(
        "R  pom.xml -> launch.json",
        true))
            .isEqualTo(" %s".formatted(coloringWord("launch.json", CL_GRAY)));
  }

  @Test
  void gitStatus_withRootModulePrefixInPath_shouldStillUseGrayColor() {
    // given:
    GisConfigMock.mockDontCareFiles("mem-console");

    // then:
    assertThat(StdOutUtils.gitStatusOneLine(
        "?? src/mem-console/",
        true,
        "src"))
            .isEqualTo(" %s".formatted(coloringWord("mem-console", CL_GRAY)));
  }

  @Test
  void print_OK() {
    // when:
    StdOutUtils.print("sysout print");

    // then:
    assertThat(outCaptor.toByteArray())
        .isEqualTo("sysout print".getBytes())
        .containsExactly(115, 121, 115, 111, 117, 116, 32, 112, 114, 105, 110, 116);
  }

  @Test
  void println_OK() {
    // when:
    StdOutUtils.println("sysout println");

    // then:
    assertThat(outCaptor.toByteArray())
        .isEqualTo("sysout println\n".getBytes())
        .containsExactly(115, 121, 115, 111, 117, 116, 32, 112, 114, 105, 110, 116, 108, 110, 10);
  }

  @Test
  void println_withMuted_NOK() {
    // given:
    StdOutUtils.setMuteOutput(true);

    // when:
    StdOutUtils.println("sysout println");

    // then:
    assertThat(outCaptor.toString()).isEmpty();
  }

  @Test
    void print_withMuted_NOK() {
    // given:
    StdOutUtils.setMuteOutput(true);

    // when:
    StdOutUtils.print("eeeeaaaaaaa");

    // then:
    assertThat(outCaptor.toString()).isEmpty();
  }
}
