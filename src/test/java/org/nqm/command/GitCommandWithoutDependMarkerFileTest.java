package org.nqm.command;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nqm.GisException;
import org.nqm.helper.StdBaseTest;

class GitCommandWithoutDependMarkerFileTest extends StdBaseTest {

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
