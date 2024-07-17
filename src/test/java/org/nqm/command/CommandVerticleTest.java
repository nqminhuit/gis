package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nqm.config.GisConfig.GIT_HOME_DIR;
import static org.nqm.utils.GisStringUtils.NEWLINE;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nqm.GisException;
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
    var projectName = "" + tempPath.subpath(1, tempPath.getNameCount());
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("On branch master", 0, projectName),
        tempPath.toFile(),
        GIT_HOME_DIR, "status");

    // when:
    var result = CommandVerticle.execute(tempPath, "status");

    // then:
    assertThat(result)
        .isEqualTo(StdOutUtils.infof("" + tempPath.getFileName()) + NEWLINE + "  On branch master");
  }

  @Test
  void executeSimpleCommand_withGisOption1_OK() {
    // given:
    var projectName = "" + tempPath.subpath(1, tempPath.getNameCount());
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("On branch master", 0, projectName),
        tempPath.toFile(),
        GIT_HOME_DIR, "status");

    // when:
    var result = CommandVerticle.execute(tempPath, "status", "--gis-no-print-modules-name");

    // then:
    assertThat(result).isEqualTo("On branch master" + NEWLINE);
  }

  @Test
  void executeSimpleCommand_withExitCode1() {
    // given:
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("On branch master", 1, ""),
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

    var projectName = "" + tempPath.subpath(1, tempPath.getNameCount());

    GisProcessUtilsMock.mockRun(
        new GisProcessDto(output, 0, projectName),
        tempPath.toFile(),
        GIT_HOME_DIR, "diff", "--name-only");

    // when:
    var result = CommandVerticle.execute(tempPath, "diff", "--name-only", "--gis-concat-modules-name");

    // then:
    assertThat(result).isEqualTo("""
        %s/%s
        %s/%s
        %s/%s
        """.formatted(
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
    var projectName = "" + tempPath.subpath(1, tempPath.getNameCount());
    GisProcessUtilsMock.mockRun(
        new GisProcessDto("Already up to date.", 0, projectName),
        tempPath.toFile(),
        GIT_HOME_DIR, "pull");

    // when:
    var result = CommandVerticle.execute(tempPath, "pull");

    // then:
    assertThat(stripColorsToString.apply(result)).isEqualTo(
        "%s%n  Already up to date.".formatted("" + tempPath.subpath(1, tempPath.getNameCount())));
  }

  @Test
  void execute_withInterruptedException_NOK() {
    // given:
    GisProcessUtilsMock.mockRunThrowException(
        new InterruptedException("youre hackedd!!!"), tempPath.toFile(), GIT_HOME_DIR, "pull");

    // when:
    assertThatThrownBy(() -> CommandVerticle.execute(tempPath, "pull"))
        .isInstanceOf(GisException.class)
        .hasMessage("youre hackedd!!!");
  }

  @Test
  void execute_withIOException_NOK() {
    // given:
    GisProcessUtilsMock.mockRunThrowException(
        new IOException("you be hacke"), tempPath.toFile(), GIT_HOME_DIR, "pull");

    // when:
    assertThatThrownBy(() -> CommandVerticle.execute(tempPath, "pull"))
        .isInstanceOf(GisException.class)
        .hasMessage("you be hacke");
  }

}
