package org.nqm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nqm.utils.StdOutUtils.CL_RESET;
import static org.nqm.utils.StdOutUtils.CL_YELLOW;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.nqm.exception.GisException;
import org.nqm.helper.StdBaseTest;

class GisLogTest extends StdBaseTest {

  @Test
  void setIsDebugEnabled_OK() {
    // given:

    // when:
    GisLog.setIsDebugEnabled(true);
    GisLog.debug("executing '%s' under '%s'", new String[] {"a", "b"}, Path.of("/", "tmp"));

    // then:
    assertThat(outCaptor.toByteArray())
        .isEqualTo(("  " + CL_YELLOW + "[DEBUG] executing 'a b' under '/tmp'" + CL_RESET + "\n").getBytes());
  }

  @Test
  void debugMsg_NOK() {
    // given:
    GisLog.setIsDebugEnabled(false);

    // when:
    GisLog.debug("moonlight sonata");

    // then:
    assertThat(outCaptor.toString()).isEmpty();
  }

  @Test
  void debugMsg_OK() {
    // given:
    GisLog.setIsDebugEnabled(true);

    // when:
    GisLog.debug("moonlight sonata");

    // then:
    assertThat(outCaptor.toByteArray())
        .isEqualTo(("  " + CL_YELLOW + "[DEBUG] moonlight sonata" + CL_RESET + "\n").getBytes());
  }

  @Test
  void debugException_OK() {
    // given:
    var e = new GisException("youre hacked!");
    GisLog.setIsDebugEnabled(true);

    // when:
    GisLog.debug(e);

    // then:
    assertThat(errCaptor.toString().trim())
        .isEqualTo("org.nqm.exception.GisException: youre hacked!");
  }

  @Test
  void debugException_NOK() {
    // given:
    var e = new GisException("youre hacked!");

    // when:
    GisLog.debug(e);

    // then:
    assertThat(outCaptor.toString().trim()).isEmpty();
  }
}
