package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nqm.exception.GisException;
import org.nqm.helper.StdBaseTest;

class GitCommandWithoutDependMarkerFileTest extends StdBaseTest {

  private static final GitCommand GIS = new GitCommand();

  @Test
  void pull_withoutAnyMarkerFiles_NOK() {
    Assertions.assertThatThrownBy(() -> GIS.pull())
        .isInstanceOf(GisException.class)
        .hasMessage("Could not find '.gis-modules' or '.gitmodules' under this directory!");
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
}
