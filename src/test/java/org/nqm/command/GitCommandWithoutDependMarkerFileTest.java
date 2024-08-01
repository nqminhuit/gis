package org.nqm.command;

import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nqm.GisException;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.StdBaseTest;

class GitCommandWithoutDependMarkerFileTest extends StdBaseTest {

  @TempDir
  private Path tempPath;

  @Override
  protected void additionalSetup() {
    GisConfigMock.mockCurrentDirectory("" + tempPath);
  }

  @Override
  protected void additionalTeardown() {
    GisConfigMock.close();
  }

  @Test
  void pull_withoutAnyMarkerFiles_NOK() {
    // given:
    var gis = new GitCommand();

    // when then:
    Assertions.assertThatThrownBy(() -> gis.pull())
        .isInstanceOf(GisException.class)
        .hasMessage("Could not find '.gis-modules' or '.gitmodules' under this directory!");
  }
}
