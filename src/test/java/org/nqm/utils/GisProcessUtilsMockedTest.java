package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nqm.helper.StdBaseTest;

class GisProcessUtilsMockedTest extends StdBaseTest {

  @TempDir
  private Path tempPath;

  @Override
  protected void additionalTeardown() {
    GisProcessUtils.isDryRunEnabled(false);
  }

  @Test
  void run_withDryRunEnabled_OK() throws IOException, InterruptedException {
    // given:
    GisProcessUtils.isDryRunEnabled(true);

    // when:
    var result = GisProcessUtils.run(tempPath.toFile(), "asdf", "betman");

    // then:
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).isEmpty();
    assertThat(outCaptor).hasToString("""
        asdf betman
        """);
  }

  @Test
  void quickRun_withDryRunEnabled_OK() throws IOException, InterruptedException {
    // given:
    GisProcessUtils.isDryRunEnabled(true);

    // when:
    var result = GisProcessUtils.quickRun(tempPath.toFile(), "catcmd", "uio");

    // then:
    assertThat(result.exitCode()).isZero();
    assertThat(result.output()).isEmpty();
    assertThat(outCaptor).hasToString("""
        catcmd uio
        """);
  }
}
