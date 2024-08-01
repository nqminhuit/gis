package org.nqm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nqm.helper.StdBaseTest;

class GisProcessUtilsDependsOutputTest extends StdBaseTest {

  @TempDir
  private Path tempPath;

  @Test
  void executeSimpleCommand_withExitCode1() throws IOException, InterruptedException {
    // when:
    var result = GisProcessUtils.run(tempPath.toFile(), "git");

    // then:
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(errCaptor.toString().trim())
        .contains("WARNING: Could not perform on module: '%s'".formatted(tempPath.getFileName()));
  }

}
