package org.nqm;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.GitBaseTest;

class GisIntTest extends GitBaseTest {

  // private static final Logger log = LoggerFactory.getLogger(GisIntTest.class);

  @Override
  protected void additionalSetup() {
    GisConfigMock.mockCurrentDirectory("" + tempPath);
  }

  @Override
  protected void additionalTeardown() {
    GisConfigMock.close();
  }

  @Test
  void gis_init_OK() throws IOException {
    // given:
    create_clone_gitRepositories("rem7_y", "rem8_c", "rem9_m");

    // when:
    Gis.main("init");

    // then:
    var markerFile = tempPath.resolve(".gis-modules");
    assertThat(Files.exists(markerFile)).isTrue();
    assertThat(Files.readAllLines(markerFile)).containsExactlyInAnyOrder(
        "path = rem9_m",
        "path = rem8_c",
        "path = rem7_y");
  }

  @Test
  void gis_setVerbose_OK() throws IOException {
    // given:
    create_clone_gitRepositories("rem1_i", "rem2_j", "rem3_k");
    Gis.main("init");
    ignoreMarkerFile();
    git(tempPath, "init");

    // when:
    Gis.setVerbose(true);
    Gis.main();

    // then:
    var sPath = "" + tempPath;
    assertThat(stripColors.apply(outCaptor.toString())).contains(
            "  [DEBUG] executing command '/usr/bin/git status -sb --ignore-submodules --porcelain=v2' under module '%s/rem1_i'"
                .formatted(sPath),
            "  [DEBUG] executing command '/usr/bin/git status -sb --ignore-submodules --porcelain=v2' under module '%s/rem2_j'"
                .formatted(sPath),
            "  [DEBUG] executing command '/usr/bin/git status -sb --ignore-submodules --porcelain=v2' under module '%s/rem3_k'"
                .formatted(sPath),
            "  [DEBUG] executing command '/usr/bin/git status -sb --ignore-submodules --porcelain=v2' under module '%s'"
                .formatted(sPath),
            "rem1_i master",
            "rem2_j master",
            "rem3_k master",
            "%s master .gitignore rem1_i rem2_j rem3_k".formatted("" + tempPath.subpath(1, tempPath.getNameCount())));
  }

  @Test
  void gis_withoutMarkerFile_handleException() {
    // given:
    create_clone_gitRepositories("rem4_a");
    git(tempPath, "init");
    Gis.setVerbose(true);

    // when:
    Gis.main("status");

    // then:
    assertThat(stripColorsToString.apply(errCaptor.toString())).contains(
        "org.nqm.GisException: Could not find '.gis-modules' or '.gitmodules' under this directory!");
  }

  @Test
  void gis_handleFolderNotInSubmodules_OK() throws IOException {
    // given:
    create_clone_gitRepositories("rem1_i", "rem2_j", "rem3_k");
    Gis.main("init");
    git(tempPath, "init");
    Files.write(tempPath.resolve(".gis-modules"), "path = asdf".getBytes());

    // when:
    Gis.main();

    // then:
    assertThat(stripColors.apply(errCaptor.toString())).contains(
        "  ERROR: directory '%s/asdf' does not exist, will be ignored!".formatted("" + tempPath));
  }
}
