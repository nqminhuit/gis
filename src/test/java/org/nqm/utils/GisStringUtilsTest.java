package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GisStringUtilsTest {

  @Test
  void isBlank() {
    assertThat(GisStringUtils.isBlank(null)).isTrue();
    assertThat(GisStringUtils.isBlank("")).isTrue();
    assertThat(GisStringUtils.isBlank(" ")).isTrue();
    assertThat(GisStringUtils.isBlank("  ")).isTrue();

    assertThat(GisStringUtils.isBlank(".")).isFalse();
    assertThat(GisStringUtils.isBlank("asdf")).isFalse();
    assertThat(GisStringUtils.isBlank(" asdf ")).isFalse();
  }
}
