package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GisVersionTest {

  @Test
  void getVersion_OK() throws Exception {
    // given:
    var gisVersion = new GisVersion();

    // when:
    var version = gisVersion.getVersion();

    // then:
    assertThat(version).containsExactly("gis 1.2.3-dev", "commit 44e3d50");
  }
}
