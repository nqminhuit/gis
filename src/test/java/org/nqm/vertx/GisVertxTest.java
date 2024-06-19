package org.nqm.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GisVertxTest {

    @Test
    void instance_OK() {
        assertThat(GisVertx.instance()).isNotNull();
    }

    @Test
    void eventBus_OK() {
        assertThat(GisVertx.eventBus()).isNotNull();
    }

    @Test
    void eventAddDir_OK() {
        try {
            GisVertx.eventAddDir(Path.of("/tmp"));
        } catch (Exception e) {
            fail("Should not expect exception");
        }
    }

    @Test
    void eventRemoveDir_OK() {
        // given:
        var path = Path.of("/tmp");
        GisVertx.eventAddDir(path);

        GisVertx.eventAddDir(Path.of("eee")); // this is to prevent from System.exit(0)

        // when:
        try {
            GisVertx.eventRemoveDir(path);
        } catch (Exception e) {
            fail("Should not expect exception");
        }
    }
}
