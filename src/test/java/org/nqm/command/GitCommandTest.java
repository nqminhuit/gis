package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.nqm.command.GitCommand.GIS_AUTOCOMPLETE_FILE;
import static org.nqm.config.GisConfig.GIT_HOME_DIR;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nqm.helper.ExecutorsMock;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.GisProcessUtilsMock;
import org.nqm.helper.StdBaseTest;
import org.nqm.model.GisBranchStatus;
import org.nqm.model.GisFormat;
import org.nqm.model.GisModuleStatus;
import org.nqm.model.GisSort;
import org.nqm.utils.GisProcessUtils;

@ExtendWith(MockitoExtension.class)
class GitCommandTest extends StdBaseTest {

  private GitCommand gis;

  @TempDir
  private Path tempPath;

  private Path markerFile;

  @Mock
  private ExecutorService exe;

  void ignoreMarkerFile() throws IOException {
    markerFile = tempPath.resolve(".gis-modules");
    Files.createFile(markerFile);
    var gitIgnoreFile = tempPath.resolve(".gitignore");
    Files.createFile(gitIgnoreFile);
    Files.writeString(gitIgnoreFile, ".gis-modules");
  }

  @Override
  protected void additionalSetup() throws IOException {
    gis = new GitCommand();
    ignoreMarkerFile();

    try {
      GisProcessUtils.run(tempPath.toFile(), GIT_HOME_DIR, "init");

      var path1 = tempPath.resolve("submodule1");
      Files.createDirectories(path1);
      GisProcessUtils.run(path1.toFile(), GIT_HOME_DIR, "init");

      var path2 = tempPath.resolve("submodule2");
      Files.createDirectories(path2);
      GisProcessUtils.run(path2.toFile(), GIT_HOME_DIR, "init");

      var path3 = tempPath.resolve("submodule3");
      Files.createDirectories(path3);
      GisProcessUtils.run(path3.toFile(), GIT_HOME_DIR, "init");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("GisProcessUtils#run failed to execute");
    }

    Files.writeString(markerFile, """
        path = submodule1
        path = submodule2
        path = submodule3
        """);
    GisConfigMock.mockCurrentDirectory("" + tempPath);
    GisConfigMock.mockBranchesColorDefault();
  }

  @Override
  protected void additionalTeardown() throws IOException {
    var fetchTmp = Path.of("/", "tmp", "gis_fetch" + ("" + Path.of("").toAbsolutePath()).replace("/", "_"));
    Files.deleteIfExists(fetchTmp);
    GisConfigMock.close();
    GisProcessUtilsMock.close();
    ExecutorsMock.close();
  }

  @Test
  void pull_withMock_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    assertThatNoException().isThrownBy(gis::pull);
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void fetchBackground_withMock_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    GisProcessUtilsMock.mockSpawn(tempPath.toFile(), GIT_HOME_DIR, "fetch");
    GisProcessUtilsMock.mockSpawn(tempPath.resolve("submodule1").toFile(), GIT_HOME_DIR, "fetch");
    GisProcessUtilsMock.mockSpawn(tempPath.resolve("submodule2").toFile(), GIT_HOME_DIR, "fetch");
    GisProcessUtilsMock.mockSpawn(tempPath.resolve("submodule3").toFile(), GIT_HOME_DIR, "fetch");

    // when:
    gis.fetchStatus(true, null);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly("git fetch started in background");
  }

  @Test
  void statusShort_withDefaultSort_OK() throws IOException {
    // when:
    gis.status(true, null, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        tempPath.getFileName() + " master .gitignore submodule1 submodule2 submodule3",
        "submodule1 master",
        "submodule2 master",
        "submodule3 master");
  }

  @Test
  void statusShort_withModuleNameSort_OK() throws IOException {
    // when:
    gis.status(true, null, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        tempPath.getFileName() + " master .gitignore submodule1 submodule2 submodule3",
        "submodule1 master",
        "submodule2 master",
        "submodule3 master");
  }

  @Test
  void statusFull_withDefaultSort_OK() throws IOException {
    // when:
    gis.status(false, null, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        "" + tempPath.getFileName(),
        "  ## master",
        "   ? .gitignore",
        "   ? submodule1/",
        "   ? submodule2/",
        "   ? submodule3/",
        "submodule1",
        "  ## master",
        "submodule2",
        "  ## master",
        "submodule3",
        "  ## master");
  }

  @Test
  void statusFull_withSortedByModuleName_OK() throws IOException {
    // when:
    gis.status(false, GisSort.module_name, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        "" + tempPath.getFileName(),
        "  ## master",
        "   ? .gitignore",
        "   ? submodule1/",
        "   ? submodule2/",
        "   ? submodule3/",
        "submodule1",
        "  ## master",
        "submodule2",
        "  ## master",
        "submodule3",
        "  ## master");
  }

  @Test
  void statusFull_withSortedByBranchName_OK() throws IOException {
    // given:
    gis.spinOff("aaa", "submodule3");
    gis.spinOff("bbb", "submodule2");
    gis.spinOff("ccc", "submodule1");
    resetOutputStreamTest();

    // when:
    gis.status(false, GisSort.branch_name, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        "" + tempPath.getFileName(),
        "  ## master",
        "   ? .gitignore",
        "   ? submodule1/",
        "   ? submodule2/",
        "   ? submodule3/",
        "submodule3",
        "  ## aaa",
        "submodule2",
        "  ## bbb",
        "submodule1",
        "  ## ccc");
  }

  @Test
  void statusShort_withSortedByBranchName_OK() throws IOException {
    // given:
    gis.spinOff("aaa", "submodule3");
    gis.spinOff("bbb", "submodule1");
    gis.spinOff("ccc", "submodule2");
    resetOutputStreamTest();

    // when:
    gis.status(true, GisSort.branch_name, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        tempPath.getFileName() + " master .gitignore submodule1 submodule2 submodule3",
        "submodule3 aaa",
        "submodule1 bbb",
        "submodule2 ccc");
  }

  @Test
  void statusFull_withSortedByTrackingStatus_OK() throws IOException {
    // given:
    Files.createFile(tempPath.resolve("submodule2").resolve("aa1.log"));
    Files.createFile(tempPath.resolve("submodule2").resolve("aa2.log"));
    Files.createFile(tempPath.resolve("submodule2").resolve("aa3.log"));
    Files.createFile(tempPath.resolve("submodule3").resolve("aa4.log"));
    Files.createFile(tempPath.resolve("submodule1").resolve("aa5.log"));
    Files.createFile(tempPath.resolve("submodule1").resolve("aa6.log"));

    // when:
    resetOutputStreamTest();
    gis.status(false, GisSort.tracking_status, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        "" + tempPath.getFileName(),
        "  ## master",
        "   ? .gitignore",
        "   ? submodule1/",
        "   ? submodule2/",
        "   ? submodule3/",
        "submodule2",
        "  ## master",
        "   ? aa1.log",
        "   ? aa2.log",
        "   ? aa3.log",
        "submodule1",
        "  ## master",
        "   ? aa5.log",
        "   ? aa6.log",
        "submodule3",
        "  ## master",
        "   ? aa4.log");
  }

  @Test
  void statusShort_withSortedByTrackingStatus_OK() throws IOException {
    // given:
    Files.createFile(tempPath.resolve("submodule2").resolve("aa1.log"));
    Files.createFile(tempPath.resolve("submodule2").resolve("aa2.log"));
    Files.createFile(tempPath.resolve("submodule2").resolve("aa3.log"));
    Files.createFile(tempPath.resolve("submodule3").resolve("aa4.log"));
    Files.createFile(tempPath.resolve("submodule1").resolve("aa5.log"));
    Files.createFile(tempPath.resolve("submodule1").resolve("aa6.log"));

    // when:
    resetOutputStreamTest();
    gis.status(true, GisSort.tracking_status, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).containsExactly(
        tempPath.getFileName() + " master .gitignore submodule1 submodule2 submodule3",
        "submodule2 master aa1.log aa2.log aa3.log",
        "submodule1 master aa5.log aa6.log",
        "submodule3 master aa4.log");
  }

  @Test
  void status_withOneLiner_OK() throws IOException {
    // when:
    gis.status(true, GisSort.module_name, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .containsOnlyOnce(
            "submodule1 master",
            "submodule2 master",
            "submodule3 master",
            "%s master .gitignore submodule1 submodule2 submodule3"
                .formatted("" + tempPath.subpath(1, tempPath.getNameCount())));
  }

  @Test
  void fetch_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.fetchStatus(false, null);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void listBranches_withModuleName_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.listBranches(false, false);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void listBranches_withoutModuleName_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.listBranches(true, false);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void listFilesChanged_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.files();

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void remotePruneOrigin_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.remotePruneOrigin();

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void removeBranch_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.removeBranch("mastereeeee", true);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void stash_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.stash(false);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void stashPop_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.stash(true);

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void checkout_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.checkout("batabranch");

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void checkoutNewBranch_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.spinOff("batabranch");

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void checkoutNewBranch_withSpecifiedModules_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.spinOff("batabranch", "submodule1", "submodule2");

    // then:
    verify(exe, times(3)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void checkoutNewBranch_withSpecifiedModulesAndRoot_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.spinOff(
        "batabranch", "submodule1", "submodule2", "" + tempPath.subpath(1, tempPath.getNameCount()));

    // then:
    verify(exe, times(4)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void rebaseOrigin_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.rebaseOrigin("batabranch");

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void pushOrigin_withWrongAnswerToPromp_NOK() throws IOException {
    // given:
    final var systemIn = System.in;
    try {
      System.setIn(new ByteArrayInputStream("yesn't".getBytes()));

      // when:
      gis.push("batabranch", true, true, false);

      // then:
      verify(exe, times(0)).submit((Callable<?>) any());
      verify(exe, times(0)).submit((Runnable) any());
    } finally {
      System.setIn(systemIn);
    }
  }

  @Test
  void pushOrigin_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    final var in = System.in;
    try {
      System.setIn(new ByteArrayInputStream("yes".getBytes()));

      // when:
      gis.push("master", true, true, false);

      // then:
      verify(exe, times(5)).submit((Callable<?>) any());
      verify(exe, times(0)).submit((Runnable) any());
    } finally {
      System.setIn(in);
    }
  }

  @Test
  void generateCompletionToConsole_OK() throws IOException {
    // when:
    gis.generateCompletion(null);

    // then:
    assertThat(outCaptor).hasToString("""
        this is a completion
        script for test gis
        in zsh.
        """);
  }

  @Test
  void generateCompletionToFile_OK() throws IOException {
    // when:
    gis.generateCompletion(tempPath);

    // then:
    var content = Files.readString(tempPath.resolve(GIS_AUTOCOMPLETE_FILE));
    assertThat(content).isEqualTo("""
        this is a completion
        script for test gis
        in zsh.
        """);
  }

  @Test
  void generateCompletionToFile_withFileAlreadyExist_shouldOverwrite() throws IOException {
    // given:
    var file = tempPath.resolve(GIS_AUTOCOMPLETE_FILE);
    Files.createFile(file);
    Files.writeString(file, "this is some existing text");

    // when:
    gis.generateCompletion(tempPath);

    // then:
    var content = Files.readString(file);
    assertThat(content).isEqualTo("""
        this is a completion
        script for test gis
        in zsh.
        """);
  }

  @Test
  void status_withSortAndModuleWithoutGitOutput_doesNotCrash() throws IOException {
    // given: a listed module whose repository is corrupted, so git prints nothing on
    // stdout and its status entry holds nothing but the module name
    Files.createDirectories(tempPath.resolve("notagit"));
    Files.writeString(tempPath.resolve("notagit").resolve(".git"), "garbage");
    Files.writeString(markerFile, "path = notagit\n", java.nio.file.StandardOpenOption.APPEND);

    // when:
    gis.status(true, GisSort.branch_name, null);

    // then:
    assertThat(stripColors.apply(outCaptor.toString()))
        .contains("notagit", "submodule1 master", "submodule2 master", "submodule3 master");
  }

  @Test
  void sortComparator_withSubmoduleNamedLikeRoot_keepsComparatorContract() {
    // given: a submodule whose rendered basename equals the root's, so both lines share the prefix
    var root = org.nqm.utils.StdOutUtils.infof("app");
    var a = root + " master";
    var b = root + " zebra";

    // then: compare(a,b) must be consistent with -compare(b,a) instead of MIN_VALUE for both
    assertThat(GitCommand.sort(true, GisSort.branch_name, root, a, b))
        .isEqualTo(-GitCommand.sort(true, GisSort.branch_name, root, b, a));
    assertThat(GitCommand.sort(true, GisSort.branch_name, root, a, a)).isZero();
  }

  @Test
  void sortComparator_oneLineBranchSort_ignoresAnsiColors() {
    // given: one-line entries whose branch tokens carry different colors per branch kind
    var root = org.nqm.utils.StdOutUtils.infof("root");
    var master = org.nqm.utils.StdOutUtils.infof("m1")
        + " " + org.nqm.utils.StdOutUtils.CL_RED + "master" + org.nqm.utils.StdOutUtils.CL_RESET;
    var feature = org.nqm.utils.StdOutUtils.infof("m2")
        + " " + org.nqm.utils.StdOutUtils.CL_YELLOW + "feature/aaa" + org.nqm.utils.StdOutUtils.CL_RESET;
    var zeta = org.nqm.utils.StdOutUtils.infof("m3")
        + " " + org.nqm.utils.StdOutUtils.CL_GREEN + "zeta" + org.nqm.utils.StdOutUtils.CL_RESET;

    // then: ordering is alphabetical by branch name, not grouped by color
    assertThat(GitCommand.sort(true, GisSort.branch_name, root, feature, master)).isNegative();
    assertThat(GitCommand.sort(true, GisSort.branch_name, root, master, zeta)).isNegative();
    assertThat(GitCommand.sort(true, GisSort.branch_name, root, zeta, feature)).isPositive();
  }

  @Test
  void confirmYesPattern_false() throws Exception {
    // given:
    var pattern = GitCommand.CONFIRM_YES;

    // then:
    assertThat(pattern.matcher("Yey").matches()).isFalse();
    assertThat(pattern.matcher("Yas").matches()).isFalse();
    assertThat(pattern.matcher("yea").matches()).isFalse();
    assertThat(pattern.matcher("Yse").matches()).isFalse();
    assertThat(pattern.matcher("Ye!").matches()).isFalse();
    assertThat(pattern.matcher("e").matches()).isFalse();
    assertThat(pattern.matcher("E").matches()).isFalse();
    assertThat(pattern.matcher("yE").matches()).isFalse();
  }

  @Test
  void confirmYesPattern_true() throws Exception {
    // given:
    var pattern = GitCommand.CONFIRM_YES;

    // then:
    assertThat(pattern.matcher("YES").matches()).isTrue();
    assertThat(pattern.matcher("YEs").matches()).isTrue();
    assertThat(pattern.matcher("YeS").matches()).isTrue();
    assertThat(pattern.matcher("yES").matches()).isTrue();
    assertThat(pattern.matcher("yeS").matches()).isTrue();
    assertThat(pattern.matcher("yEs").matches()).isTrue();
    assertThat(pattern.matcher("Yes").matches()).isTrue();
    assertThat(pattern.matcher("yes").matches()).isTrue();
    assertThat(pattern.matcher("Y").matches()).isTrue();
    assertThat(pattern.matcher("y").matches()).isTrue();
  }

  @Test
  void status_withJsonFormat_OK() throws IOException {
    // given:
    Files.createFile(tempPath.resolve("submodule1").resolve("aa1.log"));

    // when:
    gis.status(false, null, GisFormat.json);

    // then: no ANSI color, the root module on top, then the submodules by name
    var json = outCaptor.toString();
    assertThat(json).isEqualTo(stripColorsToString.apply(json));
    assertThat(json.strip()).isEqualTo("""
        {
          "fetchedAt": null,
          "modules": [
            {
              "module": "%s",
              "root": true,
              "branch": {
                "name": "master",
                "upstream": null,
                "ahead": 0,
                "behind": 0,
                "gone": false,
                "detached": false
              },
              "files": [
                {
                  "indexStatus": "?",
                  "worktreeStatus": "?",
                  "path": ".gitignore",
                  "originalPath": null
                },
                {
                  "indexStatus": "?",
                  "worktreeStatus": "?",
                  "path": "submodule1/",
                  "originalPath": null
                },
                {
                  "indexStatus": "?",
                  "worktreeStatus": "?",
                  "path": "submodule2/",
                  "originalPath": null
                },
                {
                  "indexStatus": "?",
                  "worktreeStatus": "?",
                  "path": "submodule3/",
                  "originalPath": null
                }
              ]
            },
            {
              "module": "submodule1",
              "root": false,
              "branch": {
                "name": "master",
                "upstream": null,
                "ahead": 0,
                "behind": 0,
                "gone": false,
                "detached": false
              },
              "files": [
                {
                  "indexStatus": "?",
                  "worktreeStatus": "?",
                  "path": "aa1.log",
                  "originalPath": null
                }
              ]
            },
            {
              "module": "submodule2",
              "root": false,
              "branch": {
                "name": "master",
                "upstream": null,
                "ahead": 0,
                "behind": 0,
                "gone": false,
                "detached": false
              },
              "files": []
            },
            {
              "module": "submodule3",
              "root": false,
              "branch": {
                "name": "master",
                "upstream": null,
                "ahead": 0,
                "behind": 0,
                "gone": false,
                "detached": false
              },
              "files": []
            }
          ]
        }""".formatted(tempPath.getFileName()));
  }

  @Test
  void status_withJsonFormatAndOneLine_shouldIgnoreOneLine() throws IOException {
    // when:
    gis.status(true, null, GisFormat.json);
    var oneLine = outCaptor.toString();
    resetOutputStreamTest();
    gis.status(false, null, GisFormat.json);

    // then:
    assertThat(oneLine).isEqualTo(outCaptor.toString());
  }

  @Test
  void status_withJsonFormatAndSortByTrackingStatus_shouldPutMostChangedFirst() throws IOException {
    // given:
    Files.createFile(tempPath.resolve("submodule3").resolve("aa1.log"));
    Files.createFile(tempPath.resolve("submodule3").resolve("aa2.log"));
    Files.createFile(tempPath.resolve("submodule1").resolve("aa3.log"));

    // when:
    gis.status(false, GisSort.tracking_status, GisFormat.json);

    // then:
    var json = outCaptor.toString();
    assertThat(json.indexOf("\"submodule3\""))
        .isLessThan(json.indexOf("\"submodule1\""))
        .isLessThan(json.indexOf("\"submodule2\""));
  }

  @Test
  void status_withJsonFormatAndSortByBranchName_OK() throws IOException {
    // given:
    gis.spinOff("aaa", "submodule3");
    gis.spinOff("bbb", "submodule2");
    resetOutputStreamTest();

    // when:
    gis.status(false, GisSort.branch_name, GisFormat.json);

    // then:
    var json = outCaptor.toString();
    assertThat(json.indexOf("\"submodule3\""))
        .isLessThan(json.indexOf("\"submodule2\""))
        .isLessThan(json.indexOf("\"submodule1\""));
  }

  @Test
  void sortModules_shouldKeepRootOnTop() {
    // given:
    var root = new GisModuleStatus("zzz", true, null, java.util.List.of());
    var module = new GisModuleStatus("aaa", false, null, java.util.List.of());

    // then:
    for (var sort : new GisSort[] {null, GisSort.module_name, GisSort.branch_name, GisSort.tracking_status}) {
      assertThat(GitCommand.sortModules(sort, root, module)).isNegative();
      assertThat(GitCommand.sortModules(sort, module, root)).isPositive();
      assertThat(GitCommand.sortModules(sort, root, root)).isZero();
    }
  }

  @Test
  void sortModules_withTiedEntries_shouldFallBackToModuleName() {
    // given: the modules come back in the order their virtual thread finished, so tied entries
    // must not keep that order
    var a = new GisModuleStatus("aaa", false, new GisBranchStatus("master", null, 0, 0, false, false),
        java.util.List.of());
    var b = new GisModuleStatus("bbb", false, new GisBranchStatus("master", null, 0, 0, false, false),
        java.util.List.of());

    // then:
    assertThat(GitCommand.sortModules(GisSort.branch_name, a, b)).isNegative();
    assertThat(GitCommand.sortModules(GisSort.branch_name, b, a)).isPositive();
    assertThat(GitCommand.sortModules(GisSort.tracking_status, a, b)).isNegative();
    assertThat(GitCommand.sortModules(GisSort.tracking_status, b, a)).isPositive();
  }

  private List<String> progressReports() {
    return stripColors.apply(errCaptor.toString()).stream().filter(line -> line.startsWith("{")).toList();
  }

  private static String progressOf(String... modulesWithStatus) {
    return Stream.of(modulesWithStatus)
        .map(module -> "\"%s\":{\"status\":\"%s\"}".formatted(module.split(":")[0], module.split(":")[1]))
        .collect(java.util.stream.Collectors.joining(",", "{", "}"));
  }

  @Test
  void status_withoutProgress_shouldNotReportProgress() throws IOException {
    // when:
    gis.status(true, null, null);

    // then:
    assertThat(progressReports()).isEmpty();
  }

  @Test
  void status_withProgress_shouldReportEveryModuleOnStderr() throws IOException {
    // given:
    GitCommand.setProgressEnabled(true);
    var root = "" + tempPath.getFileName();

    // when:
    gis.status(true, null, null);

    // then: every module starts pending and ends done, with one report per state change
    var reports = progressReports();
    assertThat(reports.get(0)).isEqualTo(progressOf(
        root + ":pending", "submodule1:pending", "submodule2:pending", "submodule3:pending"));
    assertThat(reports.get(reports.size() - 1)).isEqualTo(progressOf(
        root + ":done", "submodule1:done", "submodule2:done", "submodule3:done"));
    assertThat(reports).hasSize(1 + 2 * 4)
        .allSatisfy(report -> assertThat(report)
            .contains(root, "submodule1", "submodule2", "submodule3"));
    assertThat(reports).anyMatch(report -> report.contains("in-progress"));
  }

  @Test
  void checkout_withProgress_shouldReportFailedModules() throws IOException {
    // given: none of the modules has that branch, so every git command exits non zero
    GitCommand.setProgressEnabled(true);

    // when:
    gis.checkout("no-such-branch");

    // then:
    var reports = progressReports();
    assertThat(reports.get(reports.size() - 1)).isEqualTo(progressOf(
        tempPath.getFileName() + ":failed",
        "submodule1:failed",
        "submodule2:failed",
        "submodule3:failed"));
  }

  @Test
  void status_withProgressAndModulesSharingTheirName_shouldReportThePathOfTheSecond()
      throws IOException {
    // given:
    GitCommand.setProgressEnabled(true);
    Files.createDirectories(tempPath.resolve("dup"));
    Files.createDirectories(tempPath.resolve("nested").resolve("dup"));
    Files.writeString(markerFile, """
        path = dup
        path = nested/dup
        """);

    // when:
    gis.status(true, null, null);

    // then:
    assertThat(progressReports().get(0)).isEqualTo(progressOf(
        tempPath.getFileName() + ":pending", "dup:pending", "nested/dup:pending"));
  }

  @Test
  void status_withProgressAndTheNestedModuleNamedFirst_shouldStillReportEveryModule()
      throws IOException {
    // given: the nested module takes the 'dup' name, so the top level one cannot fall back to
    // its path either, since that is the very same name
    GitCommand.setProgressEnabled(true);
    Files.createDirectories(tempPath.resolve("dup"));
    Files.createDirectories(tempPath.resolve("nested").resolve("dup"));
    Files.createDirectories(tempPath.resolve("" + tempPath.getFileName()));
    Files.writeString(markerFile, """
        path = nested/dup
        path = dup
        path = %s
        """.formatted(tempPath.getFileName()));

    // when:
    gis.status(true, null, null);

    // then: no module is lost, every one of them holds its own entry
    assertThat(progressReports().get(0)).isEqualTo(progressOf(
        tempPath.getFileName() + ":pending",
        "dup:pending",
        tempPath.resolve("dup") + ":pending",
        tempPath.resolve("" + tempPath.getFileName()) + ":pending"));
  }

  @Test
  void gisAutocompleteFileName() throws Exception {
    assertThat(GIS_AUTOCOMPLETE_FILE).isEqualTo("_gis");
  }
}
