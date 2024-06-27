package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nqm.config.GisConfig.GIT_HOME_DIR;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nqm.helper.GisProcessUtilsMock;
import org.nqm.helper.StdBaseTest;
import org.nqm.model.GisProcessDto;
import org.nqm.utils.StdOutUtils;

class CommandVerticleTest extends StdBaseTest {

  @TempDir
  private Path tempPath;

  private static UnaryOperator<Path> cutTmpRoot = p -> p.subpath(2, p.getNameCount());

  @Override
  protected void additionalTeardown() {
    GisProcessUtilsMock.close();
  }

  @Test
  void execute_withSimpleCommand_OK() {
    // given:
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("On branch master", 0),
        tempPath.toFile(),
        GIT_HOME_DIR, "status");

    // when:
    CommandVerticle.execute(tempPath, "status");

    // then:
    assertThat(outCaptor.toByteArray())
        .contains((StdOutUtils.infof("%s", "" + tempPath.getFileName()) + "\n  br master").getBytes());
  }

  @Test
  void executeSimpleCommand_withGisOption1_OK() {
    // given:
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("On branch master", 0),
        tempPath.toFile(),
        GIT_HOME_DIR, "status");

    // when:
    CommandVerticle.execute(tempPath, "status", "--gis-no-print-modules-name");

    // then:
    assertThat(outCaptor.toByteArray())
        .contains(("On branch master").getBytes());
  }

  @Test
  void executeSimpleCommand_withExitCode1() {
    // given:
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("On branch master", 1),
        tempPath.toFile(),
        GIT_HOME_DIR, "status");

    // when:
    CommandVerticle.execute(tempPath, "status", "--gis-no-print-modules-name");

    // then:
    assertThat(errCaptor.toString().trim())
        .contains("WARNING: Could not perform on module: '%s'"
            .formatted("" + tempPath.subpath(1, tempPath.getNameCount())));
  }

  @Test
  void executeSimpleCommand_withGisOption2_OK() throws IOException {
    // given:
    var f1 = Files.createTempFile(tempPath, null, "a.json");
    var f2 = Files.createTempFile(tempPath, null, "b.java");
    var f3 = Files.createTempFile(tempPath, null, "c.h");

    var f1WithoutTmpAndProject = cutTmpRoot.apply(f3);
    var f2WithoutTmpAndRoot = cutTmpRoot.apply(f2);
    var f3WithoutTmpAndRoot = cutTmpRoot.apply(f1);

    var output = """
        %s
        %s
        %s
        """.formatted(f3WithoutTmpAndRoot, f2WithoutTmpAndRoot, f1WithoutTmpAndProject);

    GisProcessUtilsMock.mockRun(
        new GisProcessDto(output, 0),
        tempPath.toFile(),
        GIT_HOME_DIR, "diff", "--name-only");

    // when:
    CommandVerticle.execute(tempPath, "diff", "--name-only", "--gis-concat-modules-name");

    // then:
    var projectName = "" + tempPath.subpath(1, tempPath.getNameCount());
    assertThat(outCaptor.toString().trim()).isEqualTo("""
        %s/%s
        %s/%s
        %s/%s"""
        .formatted(
            projectName, f3WithoutTmpAndRoot,
            projectName, f2WithoutTmpAndRoot,
            projectName, f1WithoutTmpAndProject));
  }

  @Test
  void execute_withNullPath_NOK() {
    // when:
    CommandVerticle.execute(null);

    // then:
    assertThat(outCaptor.toByteArray()).isEmpty();
  }

  @Test
  void execute_withNotStatusCommand_OK() {
    // given:
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("Already up to date.", 0),
        tempPath.toFile(),
        GIT_HOME_DIR, "pull");

    // when:
    CommandVerticle.execute(tempPath, "pull");

    // then:
    assertThat(stripColorsToString.apply(outCaptor.toString())).isEqualTo(
        "%s%n  Already up to date.%n".formatted("" + tempPath.subpath(1, tempPath.getNameCount())));
  }

}
