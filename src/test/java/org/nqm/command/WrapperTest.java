package org.nqm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.nqm.config.GisConfig.GIT_HOME_DIR;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nqm.GisException;
import org.nqm.config.GisConfig;
import org.nqm.helper.ExecutorsMock;
import org.nqm.helper.GisConfigMock;
import org.nqm.helper.GisProcessUtilsMock;
import org.nqm.helper.StdBaseTest;
import org.nqm.utils.GisProcessUtils;

@ExtendWith(MockitoExtension.class)
class WrapperTest extends StdBaseTest {

  @TempDir
  private Path tempPath;

  private Path markerFile;

  @Mock
  private ExecutorService exe;

  @Override
  protected void additionalSetup() throws IOException {
    markerFile = tempPath.resolve(".gis-modules");
    Files.createFile(markerFile);

    try {
      GisProcessUtils.run(tempPath.toFile(), GIT_HOME_DIR, "init");

      var path1 = tempPath.resolve("submodule1");
      Files.createDirectories(path1);
      GisProcessUtils.run(path1.toFile(), GIT_HOME_DIR, "init");

      var path2 = tempPath.resolve("submodule2");
      Files.createDirectories(path2);
      GisProcessUtils.run(path2.toFile(), GIT_HOME_DIR, "init");

      var path3 = tempPath.resolve("submodule3");
      Files.createDirectories(path3);
      GisProcessUtils.run(path3.toFile(), GIT_HOME_DIR, "init");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("GisProcessUtils#run failed to execute");
    }

    Files.writeString(markerFile, """
        path = submodule1/
        path = submodule2/
        path = submodule3/
        """);
    GisConfigMock.mockCurrentDirectory("" + tempPath);
  }

  @Override
  protected void additionalTeardown() {
    GisConfigMock.close();
    GisProcessUtilsMock.close();
    ExecutorsMock.close();
  }

  @Test
  void forEachModuleWith_OK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe);

    // when:
    Wrapper.forEachModuleWith(p -> true, "pull");

    // then:
    verify(exe, times(5)).submit((Callable<?>) any());
    verify(exe, times(0)).submit((Runnable) any());
  }

  @Test
  void forEachModuleWith_withInterruptedExceptionWhenGetFileMarker_NOK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe, new Future<File>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Unimplemented method 'cancel'");
      }

      @Override
      public boolean isCancelled() {
        throw new UnsupportedOperationException("Unimplemented method 'isCancelled'");
      }

      @Override
      public boolean isDone() {
        throw new UnsupportedOperationException("Unimplemented method 'isDone'");
      }

      @Override
      public File get() throws InterruptedException, ExecutionException {
        throw new InterruptedException("hehehe");
      }

      @Override
      public File get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Unimplemented method");
      }
    });

    assertThatThrownBy(() -> Wrapper.forEachModuleWith(p -> true, "pull"))
        .isInstanceOf(GisException.class)
        .hasMessage("hehehe");
  }

  @Test
  void forEachModuleWith_withExecutionExceptionWhenGetFileMarker_NOK() throws IOException {
    // given:
    ExecutorsMock.mockVirtualThreadCallable(exe, new Future<File>() {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Unimplemented method 'cancel'");
      }

      @Override
      public boolean isCancelled() {
        throw new UnsupportedOperationException("Unimplemented method 'isCancelled'");
      }

      @Override
      public boolean isDone() {
        throw new UnsupportedOperationException("Unimplemented method 'isDone'");
      }

      @Override
      public File get() throws InterruptedException, ExecutionException {
        throw new ExecutionException("hehehe", null);
      }

      @Override
      public File get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Unimplemented method 'get'");
      }
    });

    assertThatThrownBy(() -> Wrapper.forEachModuleWith(p -> true, "pull"))
        .isInstanceOf(GisException.class)
        .hasMessage("hehehe");
  }


  @Test
  void getCurrentBranchUnderPath_withNullResult_NOK() {
    // given:
    GisProcessUtilsMock.mockQuickRun(
        null,
        tempPath.toFile(),
        GisConfig.GIT_HOME_DIR, "branch", "--show-current");

    // when:
    var result = Wrapper.getCurrentBranchUnderPath(tempPath);

    // then:
    assertThat(result).isEmpty();
  }
}
