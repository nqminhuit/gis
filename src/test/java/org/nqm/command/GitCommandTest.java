package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.nqm.config.GisConfig.GIT_HOME_DIR;
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
import org.nqm.utils.GisProcessUtils;

@ExtendWith(MockitoExtension.class)
class GitCommandTest extends StdBaseTest {

  private GitCommand gis;

  @TempDir
  private Path tempPath;

  private Path markerFile;

  @Mock
  private ExecutorService exe;

  @Override
  protected void additionalSetup() throws IOException {
    gis = new GitCommand();
    markerFile = tempPath.resolve(".gis-modules");
    Files.createFile(markerFile);

    var gitIgnoreFile = tempPath.resolve(".gitignore");
    Files.createFile(gitIgnoreFile);
    Files.writeString(gitIgnoreFile, ".gis-modules");

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

    Files.writeString(markerFile, """
        path = submodule1
        path = submodule2
        path = submodule3
        """);
    GisConfigMock.mockCurrentDirectory("" + tempPath);
    GisConfigMock.mockBranchesColorDefault();
  }

  @Override
  protected void additionalTeardown() {
    GisConfigMock.close();
    GisProcessUtilsMock.close();
    ExecutorsMock.close();
  }

  @Test
  void pull_withMock_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(gis::pull);
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void status_withFullOutput_OK() {
    // when:
    gis.status(false);

    // then:
    assertThat(stripColors.apply(outCaptor.toString())).contains(
        "submodule1",
        "  ## master",
        "submodule2",
        "  ## master",
        "submodule3",
        "  ## master",
        "%s".formatted(tempPath.subpath(1, tempPath.getNameCount())),
        "  ## master",
        "   ? submodule1/",
        "   ? submodule2/",
        "   ? submodule3/");
  }

  @Test
  void status_withOneLiner_OK() {
    // when:
    gis.status(true);

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
  void fetch_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(gis::fetch);
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void listBranches_withModuleName_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.listBranches(false));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void listBranches_withoutModuleName_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.listBranches(true));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void listFilesChanged_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(gis::files);
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void remotePruneOrigin_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(gis::remotePruneOrigin);
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void localPrune_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.localPrune("master"));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void removeBranch_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.removeBranch("mastereeeee", true));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void stash_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.stash(false));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void stashPop_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.stash(true));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void checkout_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.checkout("batabranch"));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void checkoutNewBranch_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.checkoutNewBranch("batabranch"));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(4)).submit((Runnable) any());
  }

  @Test
  void checkoutNewBranch_withSpecifiedModules_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException()
        .isThrownBy(() -> gis.checkoutNewBranch("batabranch", "submodule1", "submodule2"));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(2)).submit((Runnable) any());
  }

  @Test
  void checkoutNewBranch_withSpecifiedModulesAndRoot_OK() {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);
    ExecutorsMock.mockVirtualThreadRunnable(exe);

    // when:
    assertThatNoException().isThrownBy(() -> gis.checkoutNewBranch(
        "batabranch", "submodule1", "submodule2", "" + tempPath.subpath(1, tempPath.getNameCount())));
    verify(exe, times(2)).submit((Callable<?>) any());
    verify(exe, times(3)).submit((Runnable) any());
  }

}
