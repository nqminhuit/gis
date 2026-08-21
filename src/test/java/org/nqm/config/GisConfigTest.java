package org.nqm.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.nqm.helper.StdBaseTest;

class GisConfigTest extends StdBaseTest {

  @Test
  void parseModuleTimeoutSeconds_withUnsetOrBlank_usesDefault() {
    assertThat(GisConfig.parseModuleTimeoutSeconds(null)).isEqualTo(60);
    assertThat(GisConfig.parseModuleTimeoutSeconds("  ")).isEqualTo(60);
    assertThat(errCaptor.toString()).isEmpty();
  }

  @Test
  void parseModuleTimeoutSeconds_withValidValue_OK() {
    assertThat(GisConfig.parseModuleTimeoutSeconds("30")).isEqualTo(30);
    assertThat(GisConfig.parseModuleTimeoutSeconds(" 120 ")).isEqualTo(120);
    assertThat(errCaptor.toString()).isEmpty();
  }

  @Test
  void parseModuleTimeoutSeconds_withNonNumericValue_warnsAndUsesDefault() {
    assertThat(GisConfig.parseModuleTimeoutSeconds("abc")).isEqualTo(60);
    assertThat(stripColorsToString.apply(errCaptor.toString()))
        .contains("WARNING: config 'module_timeout_seconds=abc' is not a number, falling back to 60s");
  }

  @Test
  void parseModuleTimeoutSeconds_withNonPositiveValue_warnsAndUsesDefault() {
    assertThat(GisConfig.parseModuleTimeoutSeconds("0")).isEqualTo(60);
    assertThat(GisConfig.parseModuleTimeoutSeconds("-5")).isEqualTo(60);
    assertThat(stripColorsToString.apply(errCaptor.toString()))
        .contains("WARNING: config 'module_timeout_seconds=0' must be positive, falling back to 60s")
        .contains("WARNING: config 'module_timeout_seconds=-5' must be positive, falling back to 60s");
  }
}
