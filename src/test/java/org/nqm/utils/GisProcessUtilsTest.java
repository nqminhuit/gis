package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GisProcessUtilsTest {

  @TempDir
  private Path tempPath;

  @Test
  void run_OK() throws IOException, InterruptedException {
    // when:
    var result = GisProcessUtils.run(tempPath.toFile(), "pwd");

    // then:
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).isEqualTo("" + tempPath + "%n".formatted());
  }

  @Test
  void run_NOK() throws IOException {
    assertThatThrownBy(() -> GisProcessUtils.run(tempPath.toFile(), "a___s__d_fthisisinvalidcommand"))
        .isInstanceOf(IOException.class)
        .hasMessage(
            "Cannot run program \"a___s__d_fthisisinvalidcommand\" (in directory \"%s\"): error=2, No such file or directory"
                .formatted("" + tempPath));
  }

  @Test
  void quickRun_OK() throws IOException {
    // when:
    var result = GisProcessUtils.quickRun(tempPath.toFile(), "pwd");

    // then:
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).isEqualTo("" + tempPath + "%n".formatted());
  }

  @Test
  void quickRun_NOK() throws IOException {
    assertThatThrownBy(
        () -> GisProcessUtils.quickRun(tempPath.toFile(), "a___s__d_fthisisinvalidcommand"))
            .isInstanceOf(IOException.class)
            .hasMessage(
                "Cannot run program \"a___s__d_fthisisinvalidcommand\" (in directory \"%s\"): error=2, No such file or directory"
                    .formatted("" + tempPath));
  }
}
