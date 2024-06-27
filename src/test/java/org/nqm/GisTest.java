package org.nqm;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.nqm.config.GisLog;
import org.nqm.helper.StdBaseTest;

class GisTest extends StdBaseTest {

  private Gis gis;

  @Test
  void setVerbose_OK() {
    // given:
    gis = new Gis();

    // when:
    gis.setVerbose(true);
    GisLog.debug("eee");

    // then:
    assertThat(outCaptor.toString().trim()).contains("[DEBUG] eee");
  }
}
