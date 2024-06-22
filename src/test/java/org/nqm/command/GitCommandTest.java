package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nqm.config.GisConfig;
import org.nqm.exception.GisException;

class GitCommandTest {

  private static final GitCommand GIS = new GitCommand();

  private static void withMarkerFileDo(String marker, Runnable task) {
    var file = Path.of(".", marker).toFile();
    try {
      file.createNewFile();
      task.run();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      file.delete();
    }
  }

  private String getCurrentBranchUnderPath(Path path) {
    try (BufferedReader currentBranch = new BufferedReader(
        new InputStreamReader(new ProcessBuilder(GisConfig.GIT_HOME_DIR, "branch", "--show-current")
            .directory(path.toFile())
            .start()
            .getInputStream()))) {
      return currentBranch.readLine();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Test
  void pull_withoutAnyMarkerFiles_NOK() {
    Assertions.assertThatThrownBy(() -> GIS.pull())
        .isInstanceOf(GisException.class)
        .hasMessage("Could not find '.gis-modules' or '.gitmodules' under this directory!");
  }

  @Test
  void pull_withoutInvalidMarkerFile_NOK() {
    Assertions.assertThatThrownBy(() -> withMarkerFileDo("botman", () -> GIS.pull()))
        .isInstanceOf(GisException.class)
        .hasMessage("Could not find '.gis-modules' or '.gitmodules' under this directory!");
  }

  @Test
  void pull_OK() {
    assertThatNoException()
      .isThrownBy(() -> withMarkerFileDo(".gitmodules", () -> GIS.pull()));
  }

  @Test
  void status_withFullOutput_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gitmodules", () -> GIS.status(false)));
  }

  @Test
  void status_withOnliner_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.status(true)));
  }

  @Test
  void fetch_withMarkerFileGismodules_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.fetch()));
  }

  @Test
  void listBranches_withModuleName_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.listBranches(false)));
  }

  @Test
  void listBranches_withoutModuleName_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.listBranches(true)));
  }

  @Test
  void init_OK() {
    try {
      GIS.init();
    } catch (Exception e) {
      fail("Unexpected exception: " + e.getMessage());
    } finally {
      var deleted = Path.of(".", ".gis-modules").toFile().delete();
      assertThat(deleted).isTrue();
    }
  }

  @Test
  void listFilesChanged_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.files()));
  }

  @Test
  void checkoutNewBranchOnAllSubModules_thenBackToOriginalBranch_thenRemoveNewBranch_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> {
          var originalBranch = getCurrentBranchUnderPath(Path.of("."));
          var branchTest = "_____abtest";
          GIS.checkoutNewBranch(branchTest);
          GIS.checkout(originalBranch);
          GIS.removeBranch(branchTest, true);
        }));
  }

  @Test
  void checkoutNewBranchOnSpecifiedSubModules_thenBackToOriginalBranch_thenRemoveNewBranch_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> {
          var originalBranch = getCurrentBranchUnderPath(Path.of("."));
          var branchTest = "_____abtest";
          GIS.checkoutNewBranch(branchTest, "");
          GIS.checkout(originalBranch);
          GIS.removeBranch(branchTest, true);
        }));
  }

  @Test
  void fetchOrigin_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> {
          var originalBranch = getCurrentBranchUnderPath(Path.of("."));
          GIS.fetchOrigin(originalBranch);
        }));
  }

  @Test
  void remotePruneOrigin_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.remotePruneOrigin()));
  }

  @Test
  void localPrune_OK() {
    assertThatNoException()
        .isThrownBy(() -> withMarkerFileDo(".gis-modules", () -> GIS.localPrune("master")));
  }
}
