package org.nqm.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GisConfigTest {

    @Test
    void vertxOptions_OK() {
        var opt = GisConfig.VERTX_OPTIONS;
        assertThat(opt.getEventLoopPoolSize()).isEqualTo(1);
        assertThat(opt.getWorkerPoolSize()).isEqualTo(1);
        assertThat(opt.getInternalBlockingPoolSize()).isEqualTo(1);
        assertThat(opt.getBlockedThreadCheckInterval()).isEqualTo(1_000_000);
    }
}
