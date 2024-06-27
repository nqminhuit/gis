package org.nqm.helper;

import java.io.File;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nqm.model.GisProcessDto;
import org.nqm.utils.GisProcessUtils;

public class GisProcessUtilsMock {

  private static MockedStatic<GisProcessUtils> mock;

  public static void mockRun(GisProcessDto mockResult, File directory, String... commands) {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(GisProcessUtils.class);
    }
    mock.when(() -> GisProcessUtils.run(directory, commands)).thenReturn(mockResult);
  }

  public static void mockRuns(GisProcessDto mockResult, List<File> directories, String... commands) {
    if (mock == null || mock.isClosed()) {
      mock = Mockito.mockStatic(GisProcessUtils.class);
    }
    directories.stream().forEach(
        directory -> mock.when(() -> GisProcessUtils.run(directory, commands)).thenReturn(mockResult));
  }

  public static void close() {
    if (mock != null && !mock.isClosed()) {
      mock.close();
    }
  }
}
