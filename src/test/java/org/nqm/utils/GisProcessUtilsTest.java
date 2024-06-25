package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GisProcessUtilsTest {

  @TempDir
  private Path tempPath;

  @Test
  void run_OK() {
    // when:
    var result = GisProcessUtils.run(tempPath.toFile(), "pwd");

    // then:
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).isEqualTo("" + tempPath + "%n".formatted());
  }

  @Test
  void run_NOK() {
    // when:
    var result = GisProcessUtils.run(tempPath.toFile(), "a___s__d_fthisisinvalidcommand");

    // then:
    assertThat(result).isNull();
  }

  @Test
  void quickRun_OK() {
    // when:
    var result = GisProcessUtils.quickRun(tempPath.toFile(), "pwd");

    // then:
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).isEqualTo("" + tempPath + "%n".formatted());
  }

  @Test
  void quickRun_NOK() {
    // when:
    var result = GisProcessUtils.quickRun(tempPath.toFile(), "a___s__d_fthisisinvalidcommand");

    // then:
    assertThat(result).isNull();
  }

}
