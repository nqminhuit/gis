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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nqm.helper.ExecutorsMock;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.GisProcessUtilsMock;
import org.nqm.helper.StdBaseTest;
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
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void statusShort_withDefaultSort_OK() throws IOException {
    // when:
    gis.status(true, null);

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
    gis.status(true, null);

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
    gis.status(false, null);

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
    gis.status(false, GisSort.module_name);

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
    gis.status(false, GisSort.branch_name);

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
    gis.status(true, GisSort.branch_name);

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
    gis.status(false, GisSort.tracking_status);

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
    gis.status(true, GisSort.tracking_status);

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
    gis.status(true, GisSort.module_name);

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
    gis.fetchStatus(null);

    // then:
    verify(exe, times(12)).submit((Callable<?>) any());
  }

  @Test
  void listBranches_withModuleName_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.listBranches(false, false);

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void listBranches_withoutModuleName_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.listBranches(true, false);

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void listFilesChanged_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.files();

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void remotePruneOrigin_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.remotePruneOrigin();

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void localPrune_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.localPrune("master");

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void removeBranch_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.removeBranch("mastereeeee", true);

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void stash_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.stash(false);

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void stashPop_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.stash(true);

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void checkout_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.checkout("batabranch");

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void checkoutNewBranch_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.spinOff("batabranch");

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void checkoutNewBranch_withSpecifiedModules_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.spinOff("batabranch", "submodule1", "submodule2");

    // then:
    verify(exe, times(4)).submit((Callable<?>) any());
  }

  @Test
  void checkoutNewBranch_withSpecifiedModulesAndRoot_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.spinOff(
        "batabranch", "submodule1", "submodule2", "" + tempPath.subpath(1, tempPath.getNameCount()));

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
  }

  @Test
  void rebaseOrigin_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.rebaseOrigin("batabranch");

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
  }

  @Test
  void fetchOrigin_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    gis.fetchOrigin("batabranch");

    // then:
    verify(exe, times(6)).submit((Callable<?>) any());
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
      verify(exe, times(6)).submit((Callable<?>) any());
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
  void gisAutocompleteFileName() throws Exception {
    assertThat(GIS_AUTOCOMPLETE_FILE).isEqualTo("_gis");
  }
}
