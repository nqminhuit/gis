package org.nqm;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.nqm.config.GisLog;
import org.nqm.helper.StdBaseTest;

class GisTest extends StdBaseTest {

  @Test
  void setVerbose_OK() {
    // when:
    Gis.setVerbose(true);
    GisLog.debug("eee");

    // then:
    assertThat(outCaptor.toString().trim()).contains("[DEBUG] eee");
  }
}
