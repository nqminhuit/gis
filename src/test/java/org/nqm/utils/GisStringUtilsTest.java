package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

  @Test
  void getDirName() {
    assertThat(GisStringUtils.getDirName("/tmp/abc/def")).isEqualTo("def");
    assertThat(GisStringUtils.getDirName("")).isEmpty();
    assertThat(GisStringUtils.getDirName(" ")).isEqualTo(" ");

    assertThatThrownBy(() -> GisStringUtils.getDirName("/"))
      .isInstanceOf(NullPointerException.class);
  }
}
