package org.nqm.command;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nqm.config.GisConfig.GIT_HOME_DIR;
import static org.nqm.utils.GisStringUtils.NEWLINE;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.nqm.GisException;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.GisProcessUtilsMock;
import org.nqm.helper.GitBaseTest;
import org.nqm.model.GisSort;

class GitCommandIntTest extends GitBaseTest {

  private GitCommand gis;

  @Override
  protected void additionalSetup() {
    GisConfigMock.mockCurrentDirectory("" + tempPath);
    gis = new GitCommand();
  }

  @Override
  protected void additionalTeardown() {
    GisConfigMock.close();
    GisProcessUtilsMock.close();
  }

  @Test
  void init_OK() throws IOException {
    // given:
    create_clone_gitRepositories("pub_1_w", "pub_2_r", "pub_3_p");

    // when:
    gis.init();

    // then:
    assertThat(Files.readAllLines(tempPath.resolve(".gis-modules"), UTF_8))
        .containsExactlyInAnyOrder("path = pub_1_w", "path = pub_2_r", "path = pub_3_p");
  }

  @Test
  void pull_OK() throws IOException {
    // given:
    create_clone_gitRepositories("sub_1_w", "sub_2_r", "sub_3_p");
    gis.init();

    // when:
    gis.pull();

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .containsOnlyOnce("sub_1_w", "sub_2_r", "sub_3_p");
  }

  @Test
  void fetch_OK() throws IOException {
    // given:
    create_clone_gitRepositories("sub_4_w", "sub_5_r", "sub_6_p");
    gis.init();

    // when:
    gis.fetchStatus(null);

    // then:
    var timeFetch = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
    assertThat(stripColors.apply(outCaptor.toString())).containsOnlyOnce(
        "" + tempPath.subpath(1, tempPath.getNameCount()),
        "sub_4_w master",
        "sub_5_r master",
        "sub_6_p master",
        "(fetched at: %s)".formatted(timeFetch));
  }

  @Test
  void listBranches_withEmptyModifiedFiles_OK() throws IOException {
    // given:
    create_clone_gitRepositories("sub_7_w", "sub_8_r", "sub_9_p");
    gis.init();

    // when:
    gis.listBranches(false, false);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .containsOnlyOnce("sub_7_w", "sub_8_r", "sub_9_p");
  }

  @Test
  void listBranches_withModuleNames_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("ype_7_i", "ype_8_ii", "ype_9_iii");
    commitFile(repos);
    gis.init();
    gis.spinOff("bb1");
    commitFile(repos);
    resetOutputStreamTest();

    // when:
    gis.listBranches(false, false);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .containsExactlyInAnyOrder(
            "" + tempPath.subpath(1, tempPath.getNameCount()),
            "ype_9_iii",
            "  bb1",
            "  master",
            "ype_7_i",
            "  bb1",
            "  master",
            "ype_8_ii",
            "  bb1",
            "  master");
  }

  @Test
  void listBranches_withoutModuleNames_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("opu_7_i", "opu_8_ii", "opu_9_iii");
    commitFile(repos);
    gis.init();
    gis.spinOff("bb1");
    commitFile(repos);
    resetOutputStreamTest();

    // when:
    gis.listBranches(true, false);

    // then:
    assertThat(outCaptor.toString().trim().split(NEWLINE)).containsExactlyInAnyOrder(
        "bb1", "master", "bb1", "master", "bb1", "master");
  }

  @Test
  void listBranchesWithRemote_withoutModuleNames_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("opu_7_i", "opu_8_ii", "opu_9_iii");
    commitFile(repos);
    gis.init();
    gis.spinOff("bb1");
    commitFile(repos);
    System.setIn(new ByteArrayInputStream("y".getBytes()));
    gis.push("bb1", true, true, false);

    gis.spinOff("bb2");
    commitFile(repos);
    System.setIn(new ByteArrayInputStream("yeS".getBytes()));
    gis.push("bb2", true, true, false);
    resetOutputStreamTest();

    // when:
    gis.listBranches(true, true);

    // then:
    assertThat(outCaptor.toString().trim().split(NEWLINE))
        .containsExactlyInAnyOrder(
            "bb1",
            "bb2",
            "master",
            "origin/bb1",
            "origin/bb2",
            "bb1",
            "bb2",
            "master",
            "origin/bb1",
            "origin/bb2",
            "bb1",
            "bb2",
            "master",
            "origin/bb1",
            "origin/bb2");
  }

  @Test
  void listFilesChanged_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("otq_1_a", "otq_2_b", "otq_3_c");
    gis.init();
    scrambleFiles(repos);
    resetOutputStreamTest();

    // when:
    gis.files();

    // then:
    assertThat(outCaptor.toString().trim().split(NEWLINE)).containsExactlyInAnyOrder(
        "otq_1_a/filescramble1",
        "otq_3_c/filescramble1",
        "otq_2_b/filescramble1");
  }

  @Test
  void removeBranch_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("qqq_7_i", "qqq_8_ii", "qqq_9_iii");
    commitFile(repos);
    gis.init();
    gis.spinOff("new_master");
    commitFile(repos);
    resetOutputStreamTest();

    // when:
    System.setIn(new ByteArrayInputStream("Y".getBytes()));
    gis.removeBranch("master", true);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .map(s -> s.replaceFirst("\\(.*\\)", ""))
        .contains(
            "qqq_8_ii",
            "  Deleted branch master .",
            "qqq_7_i",
            "  Deleted branch master .",
            "qqq_9_iii",
            "  Deleted branch master .");
  }

  @Test
  void checkoutNewBranch_OK() throws IOException {
    // given:
    create_clone_gitRepositories("two_1_i", "two_2_ii", "two_3_iii");
    gis.init();

    // when:
    gis.spinOff("nwebra");

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).contains("two_1_i", "two_2_ii", "two_3_iii");
  }

  @Test
  void checkoutNewBranch_withSpecifiedModules_OK() throws IOException {
    // given:
    create_clone_gitRepositories("two_1_y", "two_2_yy", "two_3_yyy");
    gis.init();

    // when:
    gis.spinOff("batabranch", "two_2_yy", "two_3_yyy");

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).contains("two_2_yy", "two_3_yyy");
  }

  @Test
  void checkoutNewBranch_withSpecifiedModulesAndRoot_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("two_1_h", "two_2_hh", "two_3_hhh");
    commitFile(repos);
    git(tempPath, "init");
    repos.stream().forEach(repo -> git(tempPath, "submodule", "add", "" + repo));

    // when:
    var rootModule = "" + tempPath.subpath(1, tempPath.getNameCount());
    gis.spinOff("batabranch", "two_2_hh", rootModule);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).contains("two_2_hh", rootModule);
  }

  @Test
  void pushOrigin_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("xcom_1_h", "xcom_2_hh", "xcom_3_hhh");
    commitFile(repos);
    gis.init();
    resetOutputStreamTest();

    // when:
    System.setIn(new ByteArrayInputStream("YES".getBytes()));
    gis.push("master", true, true, false);

    // then:
    var out = Optional.of(outCaptor.toString())
        .map(s -> s.substring("Sure you want to push to remote 'master' [Y/n] ".length(), s.length()))
        .orElse("");
    assertThat(stripColors.apply(out)).contains(
        "xcom_1_h",
        "  branch 'master' set up to track 'origin/master'.",
        "xcom_2_hh",
        "  branch 'master' set up to track 'origin/master'.",
        "xcom_3_hhh",
        "  branch 'master' set up to track 'origin/master'.");
  }

  @Test
  void pushOrigin_withoutUserPrompt_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("hori_1_h", "hori_2_hh", "hori_3_hhh");
    commitFile(repos);
    gis.init();
    resetOutputStreamTest();

    // when:
    gis.push("master", true, true, true);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).contains(
        "hori_1_h",
        "  branch 'master' set up to track 'origin/master'.",
        "hori_2_hh",
        "  branch 'master' set up to track 'origin/master'.",
        "hori_3_hhh",
        "  branch 'master' set up to track 'origin/master'.");
  }

  @Test
  void pushOrigin_withoutSettingRemote_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("xcom_1_h", "xcom_2_hh", "xcom_3_hhh");
    commitFile(repos);
    gis.init();
    resetOutputStreamTest();

    // when:
    System.setIn(new ByteArrayInputStream("Yes".getBytes()));
    gis.push("master", true, false, false);

    // then:
    var out = Optional.of(outCaptor.toString())
        .map(s -> s.substring("Sure you want to push to remote 'master' [Y/n] ".length(), s.length()))
        .orElse("");
    assertThat(stripColors.apply(out)).containsExactlyInAnyOrder("xcom_1_h", "xcom_2_hh", "xcom_3_hhh");
  }

  @Test
  void pushOrigin_withSpecifiedRepos_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("batpo_1_h", "batpo_2_hh", "batpo_3_hhh");
    gis.init();
    gis.spinOff("batabranch", "batpo_1_h", "batpo_3_hhh");
    commitFile(repos);
    resetOutputStreamTest();

    // when:
    System.setIn(new ByteArrayInputStream("y".getBytes()));
    gis.push("batabranch", false, false, false);

    // then:
    var out = Optional.of(outCaptor.toString())
        .map(s -> s.substring("Sure you want to push to remote 'batabranch' [Y/n] ".length(), s.length()))
        .orElse("");
    assertThat(stripColors.apply(out))
        .doesNotContain("batpo_2_hh")
        .hasSize(2)
        .contains("batpo_1_h", "batpo_3_hhh");
  }

  @Test
  void pushOrigin_withIOException_NOK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("batpo_1_h", "batpo_2_hh", "batpo_3_hhh");
    gis.init();
    gis.spinOff("batabranch", "batpo_1_h", "batpo_3_hhh");
    commitFile(repos);
    resetOutputStreamTest();
    GisProcessUtilsMock.mockQuickRunThrowException(new IOException("nope!!,"), tempPath.toFile(),
        GIT_HOME_DIR, "branch", "--show-current");

    // when + then:
    System.setIn(new ByteArrayInputStream("Y".getBytes()));
    assertThatThrownBy(() -> gis.push("batabranch", false, false, false))
        .isInstanceOf(GisException.class)
        .hasMessage("nope!!,");
  }

  @Test
  void checkout_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("tppo_1_b", "tppo_2_bb", "tppo_3_bbb");
    gis.init();
    gis.spinOff("batabranch");
    commitFile(repos);
    gis.spinOff("master");
    commitFile(repos);

    // when:
    gis.checkout("batabranch");
    resetOutputStreamTest();

    // then:
    gis.status(true, GisSort.module_name);
    assertThat(stripColors.apply(outCaptor.toString())).contains(
        "tppo_1_b batabranch",
        "tppo_2_bb batabranch",
        "tppo_3_bbb batabranch",
        "" + tempPath.subpath(1, tempPath.getNameCount()));
  }

  @Test
  void fetchOrigin_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("po_1_z", "po_2_zz", "po_3_zzz");
    gis.init();
    gis.spinOff("batabranch");
    commitFile(repos);
    System.setIn(new ByteArrayInputStream("yes".getBytes()));
    gis.push("batabranch", true, true, false);
    gis.spinOff("master");
    commitFile(repos);

    // when:
    resetOutputStreamTest();
    gis.fetchOrigin("batabranch");

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .containsExactlyInAnyOrder("po_1_z", "po_2_zz", "po_3_zzz",
            "" + tempPath.subpath(1, tempPath.getNameCount()));
  }

  @Test
  void stash_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("pom_1_x", "pom_2_xx", "pom_3_xxx");
    gis.init();
    gis.spinOff("batabranch");
    commitFile(repos);
    scrambleFiles(repos);
    resetOutputStreamTest();
    gis.status(true, GisSort.module_name);
    assertThat(stripColors.apply(outCaptor.toString()))
        .contains(
            "" + tempPath.subpath(1, tempPath.getNameCount()),
            "pom_2_xx batabranch filescramble1",
            "pom_1_x batabranch filescramble1",
            "pom_3_xxx batabranch filescramble1");

    // when:
    resetOutputStreamTest();
    gis.stash(false);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .map(s -> s.replaceFirst(":.*", ""))
        .containsExactlyInAnyOrder(
            "" + tempPath.subpath(1, tempPath.getNameCount()),
            "pom_3_xxx",
            "  Saved working directory and index state WIP on batabranch",
            "pom_2_xx",
            "  Saved working directory and index state WIP on batabranch",
            "pom_1_x",
            "  Saved working directory and index state WIP on batabranch");
  }

  @Test
  void stashPop_OK() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("pja_4_x", "pja_5_xx", "pja_6_xxx");
    gis.init();
    gis.spinOff("batabranch");
    commitFile(repos);
    scrambleFiles(repos);
    gis.stash(false);
    resetOutputStreamTest();

    // when:
    gis.stash(true);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .map(String::trim)
        .containsExactlyInAnyOrder(
            "" + tempPath.subpath(1, tempPath.getNameCount()),
            "pja_6_xxx",
            "On branch batabranch",
            "Changes to be committed:",
            "(use \"git restore --staged <file>...\" to unstage)",
            "new file:   filescramble1",
            "pja_4_x",
            "On branch batabranch",
            "Changes to be committed:",
            "(use \"git restore --staged <file>...\" to unstage)",
            "new file:   filescramble1",
            "pja_5_xx",
            "On branch batabranch",
            "Changes to be committed:",
            "(use \"git restore --staged <file>...\" to unstage)",
            "new file:   filescramble1");
  }

  @Test
  void rebaseCurrentOrigin() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("ali_4_x", "ali_5_xx", "ali_6_xxx");
    gis.init();
    commitFile(repos);
    commitFile(repos);
    gis.push("master", true, true, true);
    resetHead1(repos);
    cleanUntrackedFiles(repos);
    resetOutputStreamTest();

    gis.status(true, null);
    assertThat(stripColors.apply(outCaptor.toString())).containsOnly(
        "" + tempPath.subpath(1, tempPath.getNameCount()),
        "ali_4_x master[behind 1]",
        "ali_5_xx master[behind 1]",
        "ali_6_xxx master[behind 1]");

    // when:
    resetOutputStreamTest();
    gis.rebaseCurrentOrigin();

    // then:
    gis.status(true, null);
    assertThat(stripColors.apply(outCaptor.toString())).containsOnly(
        "" + tempPath.subpath(1, tempPath.getNameCount()),
        "ali_4_x master",
        "ali_5_xx master",
        "ali_6_xxx master");
  }

  @Test
  void rebaseCurrentOrigin_withEachModuleHasDifferentBranch() throws IOException {
    // given:
    var repos = create_clone_gitRepositories("ali_4_x", "ali_5_xx", "ali_6_xxx");
    gis.init();
    commitFile(repos);
    gis.push("master", true, true, true);
    gis.spinOff("bbb4", "ali_4_x");
    gis.spinOff("bbb5", "ali_5_xx");
    gis.spinOff("bbb6", "ali_6_xxx");
    commitFile(repos);
    gis.push("bbb4", true, true, true);
    gis.push("bbb5", true, true, true);
    gis.push("bbb6", true, true, true);
    resetHead1(repos);
    cleanUntrackedFiles(repos);
    resetOutputStreamTest();

    gis.status(true, null);
    assertThat(stripColors.apply(outCaptor.toString())).containsOnly(
        "" + tempPath.subpath(1, tempPath.getNameCount()),
        "ali_4_x bbb4[behind 1]",
        "ali_5_xx bbb5[behind 1]",
        "ali_6_xxx bbb6[behind 1]");

    // when:
    resetOutputStreamTest();
    gis.rebaseCurrentOrigin();

    // then:
    gis.status(true, null);
    assertThat(stripColors.apply(outCaptor.toString())).containsOnly(
        "" + tempPath.subpath(1, tempPath.getNameCount()),
        "ali_4_x bbb4",
        "ali_5_xx bbb5",
        "ali_6_xxx bbb6");
  }

}
