package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nqm.utils.StdOutUtils.CL_CYAN;
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
    assertThat(StdOutUtils.gitStatus("# branch.oid f0f19fb4542365e0d7aa7d6be5187e23a258a20c"))
        .isEmpty();
    assertThat(StdOutUtils.gitStatus("# branch.head master"))
        .isEqualTo("\n  ## %s".formatted(coloringWord("master", CL_GREEN)));
    assertThat(StdOutUtils.gitStatus("# branch.upstream origin/master"))
        .isEqualTo("...%s".formatted(coloringWord("origin/master", CL_RED)));
    assertThat(StdOutUtils.gitStatus("# branch.ab +0 -0"))
        .isEmpty();
    assertThat(StdOutUtils.gitStatus("# branch.ab +2 -9"))
        .isEqualTo(
            " [ahead %s, behind %s]".formatted(coloringWord("2", CL_GREEN), coloringWord("9", CL_RED)));
    assertThat(StdOutUtils.gitStatus("# branch.ab +10 -0"))
        .isEqualTo(" [ahead %s]".formatted(coloringWord("10", CL_GREEN)));
    assertThat(StdOutUtils.gitStatus("# branch.ab +0 -20"))
        .isEqualTo(" [behind %s]".formatted(coloringWord("20", CL_RED)));
    assertThat(StdOutUtils.gitStatus(
        "1 .M N... 100644 100644 100644 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 pom.xml"))
            .isEqualTo("\n  .%s pom.xml".formatted(coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatus(
        "1 M. N... 100644 100644 100644 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 pom.xml"))
            .isEqualTo("\n  %s. pom.xml".formatted(coloringWord("M", CL_GREEN)));
    assertThat(StdOutUtils.gitStatus(
        "1 MM N... 100644 100644 100644 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 pom.xml"))
            .isEqualTo("\n  %s%s pom.xml".formatted(coloringWord("M", CL_GREEN), coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatus(
        "1 AM N... 000000 100644 100644 0000000000000000000000000000000000000000 266d4a9eb53eff40687ab923a152a879cd558ad6 src/test/java/org/nqm/utils/StdOutUtilsTest.java"))
            .isEqualTo("\n  %s%s src/test/java/org/nqm/utils/StdOutUtilsTest.java"
                .formatted(coloringWord("A", CL_GREEN), coloringWord("M", CL_RED)));
    assertThat(StdOutUtils.gitStatus(
        "2 R. N... 100644 100644 100644 7acecdb0f896b00aa0caa46532e4c59389d8edc6 7acecdb0f896b00aa0caa46532e4c59389d8edc6 R100 text-0001\ttext-1"))
            .isEqualTo("\n  %s. text-1 -> text-0001".formatted(coloringWord("R", CL_GREEN)));
  }

  @Test
  void gitStatusOneLine_OK() {
    assertThat(StdOutUtils.gitStatusOneLine("# branch.oid f0f19fb4542365e0d7aa7d6be5187e23a258a20c"))
        .isEmpty();
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head master"))
        .isEqualTo(" %s".formatted(coloringWord("master", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head main"))
        .isEqualTo(" %s".formatted(coloringWord("main", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head develop"))
        .isEqualTo(" %s".formatted(coloringWord("develop", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head feature/destroy-d-b"))
        .isEqualTo(" %s".formatted(coloringWord("feature/destroy-d-b", CL_YELLOW)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head NO-TICKETeee"))
        .isEqualTo(" %s".formatted(coloringWord("NO-TICKETeee", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.upstream origin/master")).isEmpty();
    assertThat(StdOutUtils.gitStatusOneLine("# branch.ab +0 -0"))
        .isEmpty();
    assertThat(StdOutUtils.gitStatusOneLine("# branch.ab +2 -9"))
        .isEqualTo(
            "[ahead %s, behind %s]".formatted(coloringWord("2", CL_GREEN), coloringWord("9", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.ab +10 -0"))
        .isEqualTo("[ahead %s]".formatted(coloringWord("10", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.ab +0 -20"))
        .isEqualTo("[behind %s]".formatted(coloringWord("20", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine(
        "1 .M N... 100644 100644 100644 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 pom.xml"))
            .isEqualTo(" pom.xml");
    assertThat(StdOutUtils.gitStatusOneLine(
        "1 M. N... 100644 100644 100644 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 pom.xml"))
            .isEqualTo(" pom.xml");
    assertThat(StdOutUtils.gitStatusOneLine(
        "1 MM N... 100644 100644 100644 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 2e86d6778dfa1ac74ea4db9035d8559d2c164c90 pom.xml"))
            .isEqualTo(" pom.xml");
    assertThat(StdOutUtils.gitStatusOneLine(
        "1 AM N... 000000 100644 100644 0000000000000000000000000000000000000000 266d4a9eb53eff40687ab923a152a879cd558ad6 src/test/java/org/nqm/utils/StdOutUtilsTest.java"))
            .isEqualTo(" StdOutUtilsTest.java");
  }

  @Test
  void gitStatusOneLine_withConfigBranchPrefixes_shouldCompareCaseSensitive() {
    // given:
    GisConfigMock.mockBranchesColorDefault(new String[]{"Master", "MAIN"}, new String[]{"FEA-ture"});

    // then:
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head Master"))
        .isEqualTo(" %s".formatted(coloringWord("Master", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head masTer"))
        .isEqualTo(" %s".formatted(coloringWord("masTer", CL_GREEN)));

    assertThat(StdOutUtils.gitStatusOneLine("# branch.head MAIN"))
        .isEqualTo(" %s".formatted(coloringWord("MAIN", CL_RED)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head main"))
        .isEqualTo(" %s".formatted(coloringWord("main", CL_GREEN)));

    assertThat(StdOutUtils.gitStatusOneLine("# branch.head FEA-ture/destroy-d-b"))
        .isEqualTo(" %s".formatted(coloringWord("FEA-ture/destroy-d-b", CL_YELLOW)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head Fea-ture/destroy-d-b"))
        .isEqualTo(" %s".formatted(coloringWord("Fea-ture/destroy-d-b", CL_GREEN)));
    assertThat(StdOutUtils.gitStatusOneLine("# branch.head fea-ture/destroy-d-b"))
        .isEqualTo(" %s".formatted(coloringWord("fea-ture/destroy-d-b", CL_GREEN)));
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
